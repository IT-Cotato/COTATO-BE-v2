# GCP 홈페이지 API 시간대 유지

출석 시간 비교에 사용하는 백엔드의 기본 시간대를 `Asia/Seoul`로 유지하는 수동 배포 진입점입니다. 프론트 자동 갱신·인증·출석·점수 계산 코드는 변경하지 않습니다. 기존 SSH·Compose 워크플로우와 별도로 사용하는 GCP 경로이며 자동 배포를 추가하지 않습니다.

## 설정과 경계

- 루트 `Dockerfile`은 컨테이너 `TZ`와 Java 기동 인자에 한국 시간대를 명시합니다.
- `homepage_api_deploy.py`는 새 이미지 기본 환경 아래에 기존 운영 환경을 병합하고, `TZ`와 `JAVA_TOOL_OPTIONS`의 시간대 설정을 적용합니다. 기존 JVM 옵션·인증·DB 연결 값은 유지하며 별도 파일이나 로그로 내보내지 않습니다.
- `JDK_JAVA_OPTIONS`, `JAVA_OPTS`, Java 기동 인자에 다른 시간대가 명시돼 있으면 기존 API를 중지하기 전에 거부합니다.
- Linux AMD64, Python 3.9 이상, 로컬 Docker 소켓과 API 호스트 포트 `13702`를 사용합니다. 기존 JAR 실행 구조·자원·보안·네트워크 설정을 계승하므로 새 이미지도 기존 JAR 경로와 호환되어야 합니다.
- 이 명령을 사용하는 배포에서 검사합니다. 다른 수동 `docker run`이나 기존 Compose 배포에 정책을 자동 주입하는 기능은 아닙니다.

## 설치·사용

스크립트를 관리자가 소유하는 `/usr/local/libexec/cotato-homepage-api-deploy.py`에 설치하고 `/usr/local/sbin/cotato-homepage-api-deploy`로 연결합니다. 새 이미지는 사전에 적재한 불변 `sha256` ID로 지정합니다.

```bash
sudo cotato-homepage-api-deploy --image sha256:<64자리_ID> --release <릴리즈명> --check
sudo cotato-homepage-api-deploy --image sha256:<64자리_ID> --release <릴리즈명> --verify-candidate
sudo cotato-homepage-api-deploy --image sha256:<64자리_ID> --release <릴리즈명> --apply
```

| 모드 | 동작 |
|---|---|
| 생략·`--check` | 기존 API와 이미지·준비할 시간대 설정만 읽습니다. 컨테이너를 만들지 않습니다. |
| `--verify-candidate` | 시작하지 않는 후보를 만들어 실제 Docker 설정을 검사하고 제거합니다. 앱·DB에 접속하는 후보 JVM은 실행하지 않습니다. |
| `--apply` | 명시적으로 API를 교체합니다. 상태 확인 실패·응답 유실 시 실제 상태를 조회해 이전 API를 복구하고, 복구 확인 실패 시 두 컨테이너를 보존합니다. |

운영에 도구만 설치하는 작업과 실제 앱 배포는 별도입니다. `--apply`는 기존 앱의 정상적인 시작 동작을 실행하므로, 새 버전의 DB 마이그레이션·기동 작업 검토를 대신하지 않습니다. 이 도구 자체는 SQL·마이그레이션 명령을 실행하지 않습니다.

## 검증

```bash
python3 -m unittest discover -s deployment/gcp/tests -p 'test_*.py' -v
```

단위 검사는 합성 환경값과 Docker 응답을 사용합니다. 실제 JVM 시간대는 운영 DB와 연결하지 않는 별도 Java 17 컨테이너로 검증합니다. 운영에서는 현재 컨테이너를 유지한 채 읽기 검사만 수행할 수 있습니다. 실행 결과 로그는 저장소 밖에 보관하고 커밋하지 않습니다.
