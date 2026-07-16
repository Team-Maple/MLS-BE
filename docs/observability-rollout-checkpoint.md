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
- 현재 활성 `/opt/mapleland/update-api.sh` SHA-256: `666c5e6d4bdfc51740bbc1f0bb3c7f07a9644d6798e4181aa7cd0923fa103973`
- 현재 활성 `/opt/mapleland/preflight-host.sh` SHA-256: `20b9f41dc6256457f2e736818d429ed537a409f47ff71ef3dc7348588af677b2`; Compose 2.38.2가 `create_host_path: false`를 JSON에서 `null`로 생략해 첫 실행이 이 한 조건에서 fail closed했다. 서비스 mutation은 없었고 보강본 설치 대기 상태다.
- 현재 활성 `/opt/mapleland/docker-compose.observability.yml` SHA-256: `7a0a1f77815f19940b71f07921c7411fb91d12336df93046a6de3c676ef9e8c1`
- root-only rollback backup: `/root/mapleland-observability-20260716T063108Z-1466ebe`; `/root/mapleland-observability-rollback.env`와 exact previous-image tag 생성 완료
- 현재 운영 app image ID: `sha256:1b9b13b75debfe76ad755f618738bd63972a270b78d03f90d50133ee277fa3af`
- 현재 운영 app 시작 시각: `2026-07-14T01:13:21.364861697Z`; 상태 `running`
- Alloy: 설치 완료, `active`; 애플리케이션 Observability image는 아직 미배포
- base Compose는 `root:root 0640`, `.env`는 `root:root 0600`이며 active override, update/preflight script와 `SERVICE_VERSION` 준비가 완료됐다. 실행 중 container에는 아직 적용되지 않았고 `18080` listener도 없으므로 서비스 재시작은 발생하지 않았다.
- Grafana Cloud MCP: 단일 재로그인 성공. dashboard/alert live 생성과 app metrics/ECS log 검증은 아직 완료되지 않음
- GitHub `production` Environment: 생성 완료. required reviewer는 `mungmnb777`, self-review 허용, 배포 branch는 `main`과 `feature/observability-phase-1`만 허용
- 운영 서비스 재시작, public port, firewall 변경은 실패 이후 수행하지 않음

## 다음 안전 작업

1. 최종 checkpoint commit SHA를 `.env`의 `SERVICE_VERSION`으로 갱신하고 Compose 2.38.2 보강 `preflight-host.sh`를 checksum 검증 후 재설치한다.
2. 저장소의 preflight/update/override checksum을 입력한 host preflight가 성공하는지 확인한다. 실패 원인을 해결하기 전 workflow를 재실행하지 않는다.
3. 운영 배포 영향·health/smoke·rollback·Grafana 확인 절차를 다시 보고하고 별도 승인을 받는다.
4. 승인 후에만 digest-pinned workflow를 실행하고 behavioral evidence를 이 문서와 PR에 추가한다.

## 재개 규칙

- GitHub Actions는 `host-preflight -> build-and-publish -> production approval -> host-preflight recheck -> deploy` 순서를 벗어나지 않는다.
- manual OCI 작업은 1Password 앱 잠금 상태를 추측하지 않는다. 정확한 SSH alias `OracleCloud`를 사용하고 명시적 `SSH_AUTH_SOCK`으로 `ssh-add -l`을 확인한다. 서명 승인 상태가 불명확하면 TTY에서 `ssh-add -T ~/.ssh/1Password/SHA256_QoL9bUNkoXz+boL+ozfL1CHCMCaDSZWulM8S2cVMTWs.pub`를 먼저 실행해 1Password 승인을 받은 뒤 `BatchMode` SSH 연결을 검증한다. `oracle-cloud`처럼 다른 대소문자의 host를 사용해 identity 규칙을 우회하지 않는다.
- routine deploy는 GitHub Actions의 `ORACLE_SSH_KEY`를 사용한다. 1Password SSH agent는 manual host preparation과 break-glass 확인에만 사용한다.
- Grafana Cloud MCP는 한 Codex task만 사용한다. 병렬 agent를 시작한 상태에서 Grafana MCP를 초기화하거나 OAuth 재로그인을 반복하지 않는다.
- Grafana resource는 고정 UID를 먼저 조회한 뒤 create-or-update한다. dashboard, alert, datasource, contact point, notification route 또는 MCP 연결을 blind create/delete하지 않는다.
- 샌드박스 안의 네트워크·keyring 오류만으로 사용자 재로그인을 요청하지 않는다. 같은 read-only 명령을 승인된 네트워크 경계에서 한 번 재확인한 뒤 실제 인증 오류일 때만 조치한다.
