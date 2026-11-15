#!/usr/bin/env python3
"""Preserve the homepage API runtime and require Korean time before a GCP replacement."""
import argparse
import copy
from datetime import datetime, timezone
import fcntl
import hashlib
import http.client
import json
import os
import re
import shlex
import socket
import time
from urllib.parse import quote
from urllib.request import urlopen
import uuid

ZONE = 'Asia/Seoul'
ZONE_OPTION = '-Duser.timezone=' + ZONE
API_PORT = '13702'
HEALTH = 'http://127.0.0.1:13702/actuator/health'
JVM_ENVIRONMENTS = ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_OPTS')


def emit(stage, **values):
    print(json.dumps({'stage': stage, **values}, ensure_ascii=False), flush=True)


class DockerConnection(http.client.HTTPConnection):
    """Keep Docker configuration and credentials in memory on the local VM only."""

    def __init__(self):
        super().__init__('localhost', timeout=45)

    def connect(self):
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.connect('/var/run/docker.sock')


def docker(method, path, body=None):
    connection = DockerConnection()
    try:
        connection.request(method, path, body=json.dumps(body).encode() if body is not None else None,
                           headers={'Content-Type': 'application/json'})
        response = connection.getresponse()
        raw = response.read()
        if response.status >= 300:
            raise RuntimeError('Docker operation failed; sensitive response omitted.')
        return json.loads(raw) if raw else None
    finally:
        connection.close()


def inspect(identifier):
    return docker('GET', '/containers/' + identifier + '/json')


def environment(values):
    return dict(value.split('=', 1) for value in values or [] if '=' in value)


def validate_timezone(config):
    """Reject missing policy and every explicit competing JVM timezone before stopping the API."""
    env = environment(config.get('Env'))
    if env.get('TZ') != ZONE:
        raise ValueError('The API container timezone must be Asia/Seoul.')
    option_groups = [shlex.split(env.get(name, '')) for name in JVM_ENVIRONMENTS]
    # Existing base-image entrypoint scripts are retained; the Java command remains explicit.
    startup = (config.get('Entrypoint') or []) + (config.get('Cmd') or [])
    if not any(os.path.basename(token) == 'java' for token in startup) or '-jar' not in startup:
        raise ValueError('An explicit Java JAR startup is required.')
    if ZONE_OPTION not in option_groups[0]:
        raise ValueError('The JVM timezone policy is missing from JAVA_TOOL_OPTIONS.')
    for options in [*option_groups, startup]:
        for option in options:
            if (option == '-Duser.timezone' or option.startswith('-Duser.timezone=')) and option != ZONE_OPTION:
                raise ValueError('An explicit JVM timezone conflicts with the API policy.')


def build_body(current, image, release):
    config = copy.deepcopy(current['Config'])
    # 1. Add image defaults below active runtime values; credentials are never serialized to disk.
    env = environment((image.get('Config') or {}).get('Env'))
    env.update(environment(config.get('Env')))
    env['TZ'] = ZONE
    options = env.get('JAVA_TOOL_OPTIONS', '')
    if ZONE_OPTION not in shlex.split(options):
        options = (options + ' ' + ZONE_OPTION).strip()
    env['JAVA_TOOL_OPTIONS'] = options
    config['Env'] = [name + '=' + value for name, value in env.items()]
    validate_timezone(config)
    config['Image'] = image['Id']
    labels = config.get('Labels') or {}
    labels.update({'cotato.homepage.release': release, 'cotato.homepage.component': 'api',
                   'cotato.homepage.timezone-policy': '1'})
    config['Labels'] = labels
    networks = {}
    for name, endpoint in current['NetworkSettings']['Networks'].items():
        network = {key: copy.deepcopy(value) for key, value in endpoint.items()
                   if key in ['IPAMConfig', 'Links', 'DriverOpts'] and value is not None}
        network['Aliases'] = [alias for alias in endpoint.get('Aliases') or []
                              if alias not in [current['Id'], current['Id'][:12]]]
        networks[name] = network
    return {**config, 'HostConfig': copy.deepcopy(current['HostConfig']),
            'NetworkingConfig': {'EndpointsConfig': networks}}


def validate_candidate(candidate, body, image_id):
    validate_timezone(candidate['Config'])
    if candidate['Image'] != image_id or environment(candidate['Config'].get('Env')) != environment(body['Env']):
        raise ValueError('Candidate image or environment differs from the prepared runtime.')
    for name in ['Cmd', 'Entrypoint', 'User', 'WorkingDir']:
        if candidate['Config'].get(name) != body.get(name):
            raise ValueError('Existing API startup settings were not preserved.')
    for name in ['Binds', 'Mounts', 'PortBindings', 'Memory', 'MemorySwap', 'NanoCpus',
                 'ReadonlyRootfs', 'Tmpfs', 'CapDrop', 'SecurityOpt', 'PidsLimit',
                 'RestartPolicy', 'NetworkMode', 'LogConfig', 'Dns', 'ExtraHosts']:
        if candidate['HostConfig'].get(name) != body['HostConfig'].get(name):
            raise ValueError('Existing API resource or security settings were not preserved.')


def fingerprint(container):
    value = {name: container.get(name) for name in ['Id', 'Image', 'Config', 'HostConfig', 'RestartCount']}
    value['State'] = {name: container['State'].get(name) for name in ['Running', 'StartedAt', 'OOMKilled']}
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def healthy():
    try:
        with urlopen(HEALTH, timeout=4) as response:
            return response.status == 200
    except Exception:
        return False


def wait_for_health(identifier, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if inspect(identifier)['State']['Running'] and healthy():
            return True
        time.sleep(2)
    return False


def reconcile(identifier, condition, path, body=None):
    """Re-read actual state after ambiguous Docker replies during restoration."""
    for attempt in range(3):
        try:
            if condition(inspect(identifier)):
                return
            docker('POST', path, body)
        except Exception:
            continue
    if not condition(inspect(identifier)):
        raise RuntimeError('API recovery requires administrator attention.')


def restore_previous(old, candidate, original_name, temporary_name):
    reconcile(candidate, lambda value: not value['State']['Running'],
              '/containers/' + candidate + '/stop?t=10')
    if inspect(candidate)['Name'].lstrip('/') == original_name:
        reconcile(candidate, lambda value: value['Name'].lstrip('/') == temporary_name,
                  '/containers/' + candidate + '/rename?name=' + temporary_name)
    identifier = old['Id']
    reconcile(identifier, lambda value: value['Name'].lstrip('/') == original_name,
              '/containers/' + identifier + '/rename?name=' + original_name)
    policy = old['HostConfig']['RestartPolicy']
    reconcile(identifier, lambda value: value['HostConfig']['RestartPolicy'] == policy,
              '/containers/' + identifier + '/update', {'RestartPolicy': policy})
    reconcile(identifier, lambda value: value['State']['Running'], '/containers/' + identifier + '/start')
    if not wait_for_health(identifier, 90):
        raise RuntimeError('The previous API did not return a healthy response.')
    emit('previous-api-restored')


def run(args):
    # 2. Default checks never create a container or start an application.
    image = docker('GET', '/images/' + quote(args.image, safe=':') + '/json')
    if image['Id'] != args.image or image['Os'] != 'linux' or image['Architecture'] != 'amd64':
        raise ValueError('The exact compatible API image must already be loaded.')
    containers = [inspect(item['Id']) for item in docker('GET', '/containers/json?all=true')]
    targets = [value for value in containers if value['State']['Running'] and any(
        binding['HostPort'] == API_PORT for bindings in (value['HostConfig'].get('PortBindings') or {}).values()
        for binding in bindings or [])]
    if len(targets) != 1 or not healthy():
        raise ValueError('Exactly one healthy existing homepage API is required.')
    old = targets[0]
    others = {value['Id']: fingerprint(value) for value in containers if value['Id'] != old['Id']}
    body = build_body(old, image, args.release)
    emit('timezone-preflight-passed', mode=args.mode, timezone=ZONE, existingRuntimePreserved=True)
    if args.mode == 'check':
        return
    token = datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S') + '-' + uuid.uuid4().hex[:6]
    original = old['Name'].lstrip('/')
    temporary = original + '-candidate-' + token
    backup = original + '-backup-' + token
    candidate = None
    replacement_attempted = False
    committed = False
    try:
        candidate = docker('POST', '/containers/create?name=' + temporary, body)['Id']
        validate_candidate(inspect(candidate), body, image['Id'])
        emit('candidate-timezone-verified', candidateStarted=False, timezone=ZONE)
        if args.mode == 'verify-candidate':
            return
        # 3. Only explicit --apply can replace the API. No SQL, migrations or other services are invoked.
        replacement_attempted = True
        docker('POST', '/containers/' + old['Id'] + '/update', {'RestartPolicy': {'Name': 'no'}})
        docker('POST', '/containers/' + old['Id'] + '/stop?t=20')
        docker('POST', '/containers/' + old['Id'] + '/rename?name=' + backup)
        docker('POST', '/containers/' + candidate + '/rename?name=' + original)
        docker('POST', '/containers/' + candidate + '/start')
        if not wait_for_health(candidate, 120):
            raise RuntimeError('API health verification failed.')
        validate_candidate(inspect(candidate), body, image['Id'])
        if any(fingerprint(inspect(identifier)) != value for identifier, value in others.items()):
            raise RuntimeError('An unrelated container changed during the API deployment.')
        committed = True
        emit('api-deployment-passed', timezone=ZONE, previousAPIPreserved=True, otherContainersChanged=0)
    finally:
        # 4. Recover by inspected state even when a stop/rename reply was lost after Docker applied it.
        if replacement_attempted and not committed:
            try:
                restore_previous(old, candidate, original, temporary)
            except Exception:
                emit('api-recovery-needs-attention', bothContainersPreserved=True)
                raise
        if candidate and not committed:
            docker('DELETE', '/containers/' + candidate)
            emit('temporary-candidate-removed')
        if args.mode == 'verify-candidate' and any(fingerprint(inspect(identifier)) != value
                for identifier, value in {old['Id']: fingerprint(old), **others}.items()):
            raise RuntimeError('Existing containers changed during candidate verification.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image', required=True, help='Already loaded immutable sha256 image ID.')
    parser.add_argument('--release', required=True)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument('--check', dest='mode', action='store_const', const='check')
    modes.add_argument('--verify-candidate', dest='mode', action='store_const', const='verify-candidate')
    modes.add_argument('--apply', dest='mode', action='store_const', const='apply')
    parser.set_defaults(mode='check')
    args = parser.parse_args()
    if not re.fullmatch(r'sha256:[0-9a-f]{64}', args.image) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,63}', args.release):
        parser.error('An immutable image ID and safe release label are required.')
    if os.geteuid() != 0:
        parser.error('Administrator privileges are required.')
    try:
        with open('/run/lock/cotato-homepage-api-deploy.lock', 'a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            run(args)
    except Exception as error:
        emit('api-deployment-refused-or-recovered', errorType=type(error).__name__, sensitiveDetailsOmitted=True)
        raise SystemExit(1)


if __name__ == '__main__':
    main()
