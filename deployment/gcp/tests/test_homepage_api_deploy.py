"""Verify timezone policy and replacement boundaries with synthetic Docker state only."""
import copy
import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('api_deployment', ROOT / 'homepage_api_deploy.py')
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)


def fixture():
    return {'Id': 'old', 'Name': '/synthetic-api', 'Image': 'sha256:' + '1' * 64,
            'Config': {'Image': 'sha256:' + '1' * 64,
                       'Env': ['AUTH_FIXTURE=synthetic-only', 'TZ=Asia/Seoul',
                               'JAVA_TOOL_OPTIONS=-Xmx256m -Duser.timezone=Asia/Seoul'],
                       'Entrypoint': ['/image-entrypoint.sh'],
                       'Cmd': ['java', '-jar', '/app/app.jar'], 'WorkingDir': '/app', 'User': '',
                       'Labels': {'cotato.homepage.release': 'fixture-before'}},
            'State': {'Running': True, 'StartedAt': 'before', 'OOMKilled': False},
            'RestartCount': 0,
            'HostConfig': {'Memory': 512 * 1024**2, 'MemorySwap': 512 * 1024**2,
                           'RestartPolicy': {'Name': 'unless-stopped', 'MaximumRetryCount': 0},
                           'PortBindings': {'8080/tcp': [{'HostIp': '127.0.0.1', 'HostPort': '13702'}]},
                           'Binds': ['/fixture/config:/config:ro'], 'ReadonlyRootfs': True,
                           'SecurityOpt': ['no-new-privileges'], 'CapDrop': ['ALL'],
                           'NanoCpus': 600000000, 'NetworkMode': 'fixture-network'},
            'NetworkSettings': {'Networks': {'fixture-network': {'Aliases': None}}}}


class TimezonePolicyTests(unittest.TestCase):
    """Exercise timezone-only changes while preserving all unrelated synthetic runtime values."""

    def setUp(self):
        self.current = fixture()
        self.image = {'Id': 'sha256:' + '2' * 64, 'Config': {'Env': ['NEW_DEFAULT=fixture']},
                      'Os': 'linux', 'Architecture': 'amd64'}

    def build(self):
        return deployment.build_body(self.current, self.image, 'fixture-after')

    def test_runtime_auth_startup_resources_and_network_are_preserved(self):
        before = copy.deepcopy(self.current)
        body = self.build()
        self.assertEqual(self.current, before)
        env = deployment.environment(body['Env'])
        self.assertEqual(env['AUTH_FIXTURE'], 'synthetic-only')
        self.assertEqual(env['NEW_DEFAULT'], 'fixture')
        self.assertEqual(env['JAVA_TOOL_OPTIONS'], '-Xmx256m -Duser.timezone=Asia/Seoul')
        self.assertEqual(body['HostConfig'], self.current['HostConfig'])
        for name in ['Entrypoint', 'Cmd', 'User', 'WorkingDir']:
            self.assertEqual(body[name], self.current['Config'][name])
        self.assertEqual(body['NetworkingConfig']['EndpointsConfig']['fixture-network']['Aliases'], [])

    def test_missing_timezone_is_added_without_losing_existing_flags(self):
        self.current['Config']['Env'] = ['AUTH_FIXTURE=synthetic-only', 'JAVA_TOOL_OPTIONS=-Xmx256m -Dfixture=yes']
        env = deployment.environment(self.build()['Env'])
        self.assertEqual(env['TZ'], 'Asia/Seoul')
        self.assertEqual(env['JAVA_TOOL_OPTIONS'], '-Xmx256m -Dfixture=yes -Duser.timezone=Asia/Seoul')

    def test_active_environment_overrides_new_image_defaults(self):
        self.image['Config']['Env'] += ['AUTH_FIXTURE=image-default', 'JAVA_TOOL_OPTIONS=-Xmx16m']
        env = deployment.environment(self.build()['Env'])
        self.assertEqual(env['AUTH_FIXTURE'], 'synthetic-only')
        self.assertIn('-Xmx256m', env['JAVA_TOOL_OPTIONS'])

    def test_conflicting_jvm_timezone_is_rejected_in_every_option_source(self):
        for key in deployment.JVM_ENVIRONMENTS:
            with self.subTest(environment=key):
                self.current = fixture()
                self.current['Config']['Env'].append(key + '=-Duser.timezone=UTC')
                with self.assertRaises(ValueError):
                    self.build()
        for key in ['Cmd', 'Entrypoint']:
            with self.subTest(startup=key):
                self.current = fixture()
                self.current['Config'][key].append('-Duser.timezone=UTC')
                with self.assertRaises(ValueError):
                    self.build()

    def test_missing_container_or_jvm_policy_is_rejected(self):
        for key in ['TZ', 'JAVA_TOOL_OPTIONS']:
            with self.subTest(environment=key):
                config = copy.deepcopy(self.current['Config'])
                config['Env'] = [item for item in config['Env'] if not item.startswith(key + '=')]
                with self.assertRaises(ValueError):
                    deployment.validate_timezone(config)

    def test_hidden_java_options_conflict_is_rejected_from_runtime_and_image(self):
        # Keep this input literal so a missing option-source constant cannot hide the regression.
        for source in ['runtime', 'image']:
            with self.subTest(source=source):
                self.current = fixture()
                self.image['Config']['Env'] = []
                target = self.current['Config'] if source == 'runtime' else self.image['Config']
                target['Env'].append('_JAVA_OPTIONS=-Duser.timezone=UTC')
                with self.assertRaises(ValueError):
                    self.build()

    def test_compatible_hidden_java_options_are_preserved(self):
        for options in ['-Xms16m -Dfixture=yes', '-Duser.timezone=Asia/Seoul']:
            with self.subTest(options=options):
                self.current = fixture()
                self.current['Config']['Env'].append('_JAVA_OPTIONS=' + options)
                self.assertEqual(deployment.environment(self.build()['Env'])['_JAVA_OPTIONS'], options)

    def test_unrelated_property_and_quoted_flags_are_preserved(self):
        self.current['Config']['Env'].append('JDK_JAVA_OPTIONS=-Duser.timezone.fixture=yes -Dfixture="two words"')
        self.build()

    def test_shell_only_startup_is_refused(self):
        self.current['Config']['Entrypoint'] = ['sh', '-c']
        self.current['Config']['Cmd'] = ['java -jar /app/app.jar']
        with self.assertRaises(ValueError):
            self.build()

    def test_network_options_are_retained_without_old_container_aliases(self):
        network = self.current['NetworkSettings']['Networks']['fixture-network']
        network.update({'Aliases': ['old', 'service-alias'], 'DriverOpts': {'fixture': 'yes'},
                        'IPAMConfig': {'IPv4Address': 'synthetic-only'}})
        body = self.build()['NetworkingConfig']['EndpointsConfig']['fixture-network']
        self.assertEqual(body['Aliases'], ['service-alias'])
        self.assertEqual(body['DriverOpts'], network['DriverOpts'])
        self.assertEqual(body['IPAMConfig'], network['IPAMConfig'])

    def test_candidate_detects_timezone_and_resource_drift(self):
        body = self.build()
        candidate = {'Image': self.image['Id'], 'Config': copy.deepcopy(body),
                     'HostConfig': copy.deepcopy(body['HostConfig'])}
        candidate['Config']['Env'].reverse()
        deployment.validate_candidate(candidate, body, self.image['Id'])
        for kind in ['timezone', 'memory', 'auth']:
            changed = copy.deepcopy(candidate)
            if kind == 'timezone':
                changed['Config']['Env'].append('JAVA_TOOL_OPTIONS=-Duser.timezone=UTC')
            elif kind == 'memory':
                changed['HostConfig']['Memory'] = 0
            else:
                changed['Config']['Env'].append('AUTH_FIXTURE=changed')
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                deployment.validate_candidate(changed, body, self.image['Id'])

    def test_dockerfile_declares_both_clock_bases(self):
        text = (ROOT.parents[1] / 'Dockerfile').read_text()
        self.assertIn('ENV TZ=Asia/Seoul', text)
        self.assertIn('"-Duser.timezone=Asia/Seoul"', text)


class DeploymentBoundaryTests(unittest.TestCase):
    """Model Docker responses and after-effect failures without starting Java or accessing a DB."""

    def setUp(self):
        self.old = fixture()
        self.values = {'old': copy.deepcopy(self.old)}
        self.image = {'Id': 'sha256:' + '2' * 64, 'Config': {'Env': ['NEW_DEFAULT=fixture']},
                      'Os': 'linux', 'Architecture': 'amd64'}
        self.calls = []
        self.events = []
        self.fault = lambda path: False
        self.faults = 0
        self.args = SimpleNamespace(image=self.image['Id'], release='fixture-after', mode='check')
        for name, value in [('docker', self.docker), ('healthy', lambda: True),
                            ('wait_for_health', lambda *args: True),
                            ('emit', lambda stage, **values: self.events.append(stage))]:
            replacement = patch.object(deployment, name, value)
            replacement.start()
            self.addCleanup(replacement.stop)

    def docker(self, method, path, body=None):
        self.calls.append((method, path))
        if method == 'GET' and path.startswith('/images/'):
            return copy.deepcopy(self.image)
        if method == 'GET' and path == '/containers/json?all=true':
            return [{'Id': identifier, 'Names': [value['Name']]}
                    for identifier, value in self.values.items()]
        if method == 'POST' and path.startswith('/containers/create'):
            self.values['candidate'] = {'Id': 'candidate', 'Image': body['Image'],
                'Name': '/' + path.split('name=')[1], 'Config': copy.deepcopy(body),
                'HostConfig': copy.deepcopy(body['HostConfig']), 'RestartCount': 0,
                'State': {'Running': False, 'StartedAt': '', 'OOMKilled': False}}
            return {'Id': 'candidate'}
        identifier = path.split('/')[2].split('?')[0]
        if method == 'GET':
            return copy.deepcopy(self.values[identifier])
        if method == 'DELETE':
            del self.values[identifier]
            return
        value = self.values[identifier]
        if '/update' in path:
            value['HostConfig']['RestartPolicy'] = {'MaximumRetryCount': 0, **body['RestartPolicy']}
        elif '/stop' in path:
            value['State']['Running'] = False
        elif '/rename?' in path:
            value['Name'] = '/' + path.split('name=')[1]
        elif '/start' in path:
            value['State'].update({'Running': True, 'StartedAt': 'after'})
        else:
            raise AssertionError('Unexpected synthetic operation.')
        if self.fault(path):
            self.faults += 1
            raise OSError('Synthetic lost reply after state changed.')

    def assert_old_restored(self):
        old = self.values['old']
        self.assertTrue(old['State']['Running'])
        self.assertEqual(old['Name'], self.old['Name'])
        self.assertEqual(old['Config']['Env'], self.old['Config']['Env'])
        self.assertEqual(old['HostConfig'], self.old['HostConfig'])
        self.assertNotIn('candidate', self.values)

    def test_default_check_only_reads_docker(self):
        deployment.run(self.args)
        self.assertTrue(all(method == 'GET' for method, path in self.calls))
        self.assertEqual(self.values['old'], self.old)

    def test_timezone_conflict_blocks_before_any_mutation(self):
        for key in ['JDK_JAVA_OPTIONS', '_JAVA_OPTIONS']:
            with self.subTest(environment=key):
                self.values = {'old': copy.deepcopy(self.old)}
                self.calls = []
                self.values['old']['Config']['Env'].append(key + '=-Duser.timezone=UTC')
                self.args.mode = 'apply'
                with self.assertRaises(ValueError):
                    deployment.run(self.args)
                self.assertTrue(all(method == 'GET' for method, path in self.calls))

    def test_lost_create_reply_removes_own_stopped_candidate_without_stopping_api(self):
        for mode in ['verify-candidate', 'apply']:
            with self.subTest(mode=mode):
                self.values = {'old': copy.deepcopy(self.old)}
                other = copy.deepcopy(self.old)
                other.update({'Id': 'unrelated', 'Name': '/other'})
                other['HostConfig']['PortBindings'] = {}
                self.values['unrelated'] = other
                self.calls = []
                self.args.mode = mode

                def lose_reply(method, path, body=None):
                    result = self.docker(method, path, body)
                    if method == 'POST' and path.startswith('/containers/create'):
                        raise OSError('Synthetic lost create reply.')
                    return result

                with patch.object(deployment, 'docker', lose_reply), self.assertRaises(OSError):
                    deployment.run(self.args)
                self.assertEqual(self.values['old'], self.old)
                self.assertNotIn('candidate', self.values)
                self.assertEqual(self.values['unrelated'], other)
                self.assertEqual([path for method, path in self.calls if method == 'DELETE'],
                                 ['/containers/candidate'])
                self.assertFalse(any('/start' in path or '/stop' in path for method, path in self.calls))

    def test_failure_before_create_does_not_delete_any_container(self):
        def refuse_create(method, path, body=None):
            if method == 'POST' and path.startswith('/containers/create'):
                raise OSError('Synthetic failure before create.')
            return self.docker(method, path, body)

        self.args.mode = 'verify-candidate'
        with patch.object(deployment, 'docker', refuse_create), self.assertRaises(OSError):
            deployment.run(self.args)
        self.assertEqual(self.values, {'old': self.old})
        self.assertFalse(any(method == 'DELETE' for method, path in self.calls))

    def test_ambiguous_create_preserves_foreign_running_or_changed_image_candidate(self):
        for kind in ['foreign-label', 'running', 'different-image']:
            with self.subTest(kind=kind):
                self.values = {'old': copy.deepcopy(self.old)}
                self.calls = []
                self.events = []

                def lose_reply(method, path, body=None):
                    result = self.docker(method, path, body)
                    if method == 'POST' and path.startswith('/containers/create'):
                        value = self.values['candidate']
                        if kind == 'foreign-label':
                            value['Config']['Labels']['cotato.homepage.deployment-attempt'] = 'another-attempt'
                        elif kind == 'running':
                            value['State']['Running'] = True
                        else:
                            value['Image'] = self.old['Image']
                        raise OSError('Synthetic lost create reply with unsafe candidate state.')
                    return result

                self.args.mode = 'verify-candidate'
                with patch.object(deployment, 'docker', lose_reply), self.assertRaises(Exception):
                    deployment.run(self.args)
                self.assertIn('candidate', self.values)
                self.assertEqual(self.values['old'], self.old)
                self.assertFalse(any(method == 'DELETE' for method, path in self.calls))
                self.assertIn('candidate-cleanup-needs-attention', self.events)

    def test_unavailable_candidate_lookup_reports_attention_without_stopping_api(self):
        created = False

        def unavailable_lookup(method, path, body=None):
            nonlocal created
            if created and method == 'GET' and path == '/containers/json?all=true':
                raise OSError('Synthetic Docker unavailable during recovery.')
            result = self.docker(method, path, body)
            if method == 'POST' and path.startswith('/containers/create'):
                created = True
                raise OSError('Synthetic lost create reply.')
            return result

        self.args.mode = 'verify-candidate'
        with patch.object(deployment, 'docker', unavailable_lookup), self.assertRaises(OSError):
            deployment.run(self.args)
        self.assertEqual(self.values['old'], self.old)
        self.assertIn('candidate', self.values)
        self.assertIn('candidate-cleanup-needs-attention', self.events)
        self.assertFalse(any('/start' in path or '/stop' in path for method, path in self.calls))

    def test_verify_candidate_is_never_started_and_is_removed(self):
        self.args.mode = 'verify-candidate'
        deployment.run(self.args)
        self.assertEqual(self.values['old'], self.old)
        self.assertNotIn('candidate', self.values)
        self.assertFalse(any('/start' in path or '/stop' in path for method, path in self.calls))

    def test_explicit_replacement_preserves_prior_api(self):
        self.args.mode = 'apply'
        deployment.run(self.args)
        self.assertFalse(self.values['old']['State']['Running'])
        self.assertTrue(self.values['candidate']['State']['Running'])
        deployment.validate_timezone(self.values['candidate']['Config'])
        self.assertIn('api-deployment-passed', self.events)

    def test_lost_replies_at_each_mutation_restore_previous(self):
        for fragment in ['/old/update', '/old/stop', '/old/rename', '/candidate/rename', '/candidate/start']:
            with self.subTest(operation=fragment):
                self.values = {'old': copy.deepcopy(self.old)}
                self.faults = 0
                self.fault = lambda path: fragment in path and self.faults == 0
                self.args.mode = 'apply'
                with self.assertRaises(OSError):
                    deployment.run(self.args)
                self.assert_old_restored()

    def test_failed_health_restores_old_without_changing_other_container(self):
        other = copy.deepcopy(self.old)
        other['Id'], other['Name'] = 'unrelated', '/other'
        other['HostConfig']['PortBindings'] = {}
        self.values['unrelated'] = other
        self.args.mode = 'apply'
        with patch.object(deployment, 'wait_for_health', lambda identifier, timeout: timeout == 90):
            with self.assertRaises(RuntimeError):
                deployment.run(self.args)
        self.assert_old_restored()
        self.assertEqual(self.values['unrelated'], other)

    def test_failed_recovery_preserves_both_for_manual_attention(self):
        self.args.mode = 'apply'
        with patch.object(deployment, 'wait_for_health', lambda *args: False):
            with self.assertRaises(RuntimeError):
                deployment.run(self.args)
        self.assertIn('candidate', self.values)
        self.assertIn('api-recovery-needs-attention', self.events)

    def test_missing_image_and_ambiguous_targets_block_mutation(self):
        for case in ['image', 'target']:
            with self.subTest(case=case):
                self.values = {'old': copy.deepcopy(self.old)}
                self.image = {'Id': 'sha256:' + '2' * 64, 'Config': {'Env': []},
                              'Os': 'linux', 'Architecture': 'amd64'}
                if case == 'image':
                    self.image['Id'] = 'sha256:' + '3' * 64
                else:
                    self.values['other'] = copy.deepcopy(self.old)
                    self.values['other']['Id'] = 'other'
                self.calls = []
                with self.assertRaises(ValueError):
                    deployment.run(self.args)
                self.assertTrue(all(method == 'GET' for method, path in self.calls))
                if case == 'target':
                    self.assertIn(('GET', '/containers/json?all=true'), self.calls)


if __name__ == '__main__':
    unittest.main()
