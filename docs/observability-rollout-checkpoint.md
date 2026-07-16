# Observability rollout checkpoint

이 문서는 Codex task, OAuth 또는 로컬 앱이 재시작돼도 운영 상태를 추측하거나 이미 끝난 작업을 반복하지 않기 위한 비밀값 없는 재개 기준이다. 외부 상태를 변경한 뒤에는 해당 operation의 URL·commit·checksum·검증 결과와 다음 안전 작업을 갱신한다. token, credential, 원본 환경 파일 내용은 기록하지 않는다.

## 2026-07-16 확인 상태

- Issue: [#32](https://github.com/Team-Maple/MLS-BE/issues/32)
- draft PR: [#33](https://github.com/Team-Maple/MLS-BE/pull/33)
- 관찰 기능 구현 commit: `f4bf228b934959be125a72540c91e43f003b7b6e`
- 배포 재발 방지 commit: `5975308806791e026aedfb8a93e728bce47e1450`
- 재발 방지 CI: [29475104502](https://github.com/Team-Maple/MLS-BE/actions/runs/29475104502) `success`; host-preflight test와 공식 Alloy validation 포함
- first-rollout rollback 계약 commit: `1466ebe4bcbfa89882d02973ceff4ac5b0ea50b3`
- rollback 계약 CI: [29476318762](https://github.com/Team-Maple/MLS-BE/actions/runs/29476318762) `success`; immutable deploy 성공과 legacy credential 감지 시 이전 base-contract rollback test 포함
- Docker Compose 2.38.2 false-value 정규화 호환 commit: `5c60302`; CI 확인 뒤 active preflight에 반영 예정
- 실패한 deploy run: [29470227414](https://github.com/Team-Maple/MLS-BE/actions/runs/29470227414)
- 실패한 deploy job: [87532062228](https://github.com/Team-Maple/MLS-BE/actions/runs/29470227414/job/87532062228)
- 실패 원인: 당시 활성 legacy `/opt/mapleland/update-api.sh`가 root-only `/opt/mapleland/.env`를 shell source해 `Permission denied`가 발생했다. Docker pull, container 재생성 또는 애플리케이션 재시작 전 실패했다.
- 두 번째 service-mutation 전 실패 run: [29478103871](https://github.com/Team-Maple/MLS-BE/actions/runs/29478103871). GitHub CI key의 `authorized_keys`가 `command="/opt/mapleland/update-api.sh"`를 강제해 요청한 preflight 대신 update script를 `ubuntu`로 실행한 것이 원인이다. 두 번 재현됐으며 build/pull/restart는 시작되지 않았다.
- 세 번째 service-mutation 전 실패 run: [29478686691](https://github.com/Team-Maple/MLS-BE/actions/runs/29478686691). 강제 entrypoint가 요청 명령을 stdin에서 읽었지만 OpenSSH는 이를 `SSH_ORIGINAL_COMMAND`에 제공하므로 빈 입력으로 거부했다. Build/pull/restart는 시작되지 않았다.
- 첫 실제 전환 run: [29478910973](https://github.com/Team-Maple/MLS-BE/actions/runs/29478910973). GitHub forced-command preflight와 digest-pinned build/pull은 성공했다. 후보는 실행 중이고 대표 DB API도 응답했지만 기존 management `/actuator/health` readiness 조건이 90초 안에 성공하지 않아 exact 이전 image `sha256:1b9b13b75debfe76ad755f618738bd63972a270b78d03f90d50133ee277fa3af`로 자동 롤백됐으며 공개 API 정상 상태를 재확인했다.
- readiness 보강: 전체 health는 선택적 외부 contributor 때문에 후보 생존성과 무관하게 503일 수 있다. 후보의 실제 Alloy 경계인 Bearer 인증 `/actuator/prometheus`와 대표 공개 DB API를 함께 확인하도록 변경하며, token은 curl config stdin에만 전달한다. Root health 최소 응답 계약은 통합 테스트로 유지한다.
- 두 번째 실제 전환 run: [29479980205](https://github.com/Team-Maple/MLS-BE/actions/runs/29479980205). 후보는 약 90초 동안 HTTP listener를 만들지 못했고 Docker journal에서 동일 container task가 반복 종료된 crash loop를 확인했다. `management_prometheus_status=000`, `public_smoke_status=000`이었으며 exact 이전 image로 자동 롤백 후 공개 API 200, restart count 0을 확인했다.
- 진단 보강 후보: 후보의 첫 restart를 즉시 실패로 판정하고 rollback 전에 root-only `/var/log/mapleland-deploy/last-failure/`에 state와 최근 500줄을 보존한다. CI/PR에는 원문을 출력하지 않으며 다음 진단 rollout 후 root에서만 읽고 제거한다.
- 진단 rollout: [29481404148](https://github.com/Team-Maple/MLS-BE/actions/runs/29481404148). 시작 약 1.3초 뒤 restart count 1을 포착하고 root-only 진단을 저장한 후 exact 이전 image로 약 19초 만에 복구했다. 원인은 image 내부 Firebase directory/key가 `1001:1001 0700/0600`인 반면 runtime이 `1002:1001`이라 Paketo memory calculator가 `/workspace`를 순회하지 못한 것이다.
- 권한 수정 후보: FCM restore 입력이 비어 있지 않은 service-account JSON인지 출력 없이 확인하고 `0750/0640`으로 제한한다. `zipinfo` 기준 bootJar entry가 `-rwxr-x---`/`-rw-r-----`인지 publish 전에 검증하며 directory entry는 trailing slash로 식별한다. Publish 뒤에는 shell이 없는 tiny image를 실행하지 않고 stopped container의 exported metadata에서 `Config.User=1002:1001`, directory/key `1001:1001 0750/0640`을 다시 검증해 builder UID/GID 변경도 deploy 전에 차단한다. Secret 내용과 checksum은 출력하지 않는다.
- Publish gate dry run: [29490350958](https://github.com/Team-Maple/MLS-BE/actions/runs/29490350958). FCM JSON/bootJar mode와 image publish까지 성공했지만 tiny image에 `/bin/sh`가 없어 최초 runtime read probe가 exit 127로 중단됐다. Digest resolve와 deploy job은 시작되지 않아 운영 재시작은 없었다. 검증 방식은 image를 실행하지 않는 metadata gate로 교체했다.
- 강제 command 보존 설계: `update-api.sh`의 non-root gateway가 OpenSSH의 `SSH_ORIGINAL_COMMAND`에서 정확한 단일 line `preflight <3 checksums>` 또는 `deploy <3 checksums> <immutable digest>`만 허용하고 `/usr/bin/sudo -n`으로 고정 root script를 호출한다. SSH forwarding/PTY 제한과 forced entrypoint는 그대로 유지한다.
- 현재 활성 `/opt/mapleland/update-api.sh` SHA-256: `74e714fa058a6a2318b8f842de6ef7c0582da4e460446c844af22b12e43efb26` (restart-loop fail-fast와 root-only 진단 보존 포함).
- 현재 활성 `/opt/mapleland/preflight-host.sh` SHA-256: `860fab026264378684f7d02c373fa7bc8fefb0216a97ffafc866cd5c5093e413`.
- 현재 활성 `/opt/mapleland/docker-compose.observability.yml` SHA-256: `7a0a1f77815f19940b71f07921c7411fb91d12336df93046a6de3c676ef9e8c1`
- root-only rollback backup: `/root/mapleland-observability-20260716T063108Z-1466ebe`; `/root/mapleland-observability-rollback.env`와 exact previous-image tag 생성 완료
- 현재 운영 app image ID: `sha256:1b9b13b75debfe76ad755f618738bd63972a270b78d03f90d50133ee277fa3af`
- 현재 운영 app은 exact 이전 image로 자동 롤백되어 상태 `running`; 공개 `/api/v1/jobs` smoke 성공
- Alloy: 설치 완료, `active`; 애플리케이션 Observability image는 아직 미배포
- base Compose는 `root:root 0640`, `.env`는 `root:root 0600`이며 active override, update/preflight script와 `SERVICE_VERSION` 준비가 완료됐다. 실행 중 container에는 아직 적용되지 않았고 `18080` listener도 없으므로 서비스 재시작은 발생하지 않았다.
- Grafana Cloud MCP: 기존 단일 연결 재로그인은 완료됐지만 현재 Codex 세션에는 Grafana 도구가 노출되지 않는다. 연결을 추가·삭제하지 않고 기존 로그인 세션의 UI 또는 공식 API만 fallback으로 사용한다. Dashboard/alert live 생성과 app metrics/ECS log 검증은 아직 완료되지 않음
- GitHub `production` Environment: 생성 완료. required reviewer는 `mungmnb777`, self-review 허용, 배포 branch는 `main`과 `feature/observability-phase-1`만 허용
- 실패 전환은 자동 롤백 완료; public port와 firewall 변경 없음

## 다음 안전 작업

1. Firebase artifact permission 수정과 bootJar mode 검증을 CI에서 통과시킨다.
2. 최종 commit SHA/checksum으로 active script와 `.env`를 갱신하고 production rollout을 재승인받는다.
3. 운영 재적용 후 root-only 진단을 제거하고 metrics/ECS log/rotation/Alloy 재시작과 Grafana dashboard·alert behavioral evidence를 수집한다.
4. 실패·롤백 및 최종 digest-pinned rollout evidence를 PR에 추가한다.

## 재개 규칙

- GitHub Actions는 `host-preflight -> build-and-publish -> production approval -> host-preflight recheck -> deploy` 순서를 벗어나지 않는다.
- manual OCI 작업은 1Password 앱 잠금 상태를 추측하지 않는다. 정확한 SSH alias `OracleCloud`를 사용하고 명시적 `SSH_AUTH_SOCK`으로 `ssh-add -l`을 확인한다. 서명 승인 상태가 불명확하면 TTY에서 `ssh-add -T ~/.ssh/1Password/SHA256_QoL9bUNkoXz+boL+ozfL1CHCMCaDSZWulM8S2cVMTWs.pub`를 먼저 실행해 1Password 승인을 받은 뒤 `BatchMode` SSH 연결을 검증한다. `oracle-cloud`처럼 다른 대소문자의 host를 사용해 identity 규칙을 우회하지 않는다.
- routine deploy는 GitHub Actions의 `ORACLE_SSH_KEY`를 사용한다. 1Password SSH agent는 manual host preparation과 break-glass 확인에만 사용한다.
- Grafana Cloud MCP는 한 Codex task만 사용한다. 병렬 agent를 시작한 상태에서 Grafana MCP를 초기화하거나 OAuth 재로그인을 반복하지 않는다.
- Grafana resource는 고정 UID를 먼저 조회한 뒤 create-or-update한다. dashboard, alert, datasource, contact point, notification route 또는 MCP 연결을 blind create/delete하지 않는다.
- 샌드박스 안의 네트워크·keyring 오류만으로 사용자 재로그인을 요청하지 않는다. 같은 read-only 명령을 승인된 네트워크 경계에서 한 번 재확인한 뒤 실제 인증 오류일 때만 조치한다.
