# Observability rollout checkpoint

이 문서는 Codex task, OAuth 또는 로컬 앱이 재시작돼도 운영 상태를 추측하거나 이미 끝난 작업을 반복하지 않기 위한 비밀값 없는 재개 기준이다. 외부 상태를 변경한 뒤에는 해당 operation의 URL·commit·checksum·검증 결과와 다음 안전 작업을 갱신한다. token, credential, 원본 환경 파일 내용은 기록하지 않는다.

## 2026-07-17 legacy OCI 배포 경로 복원과 운영 복구

- Owner가 성공 이력이 있는 [deploy run 28788801815](https://github.com/Team-Maple/MLS-BE/actions/runs/28788801815), commit `c49ee3255afb4ddfa0168ce91783fff368864a4d`의 legacy 배포 구조 복원을 명시적으로 승인했다. 동시 `latest-arm64` overwrite를 막는 `deploy-oci` concurrency와 실제로 쓰지 않는 `id-token: write` 제거만 적용했다.
- Workflow는 arm64 image를 `latest-arm64`로 build/publish한 뒤 Tailscale과 `appleboy/ssh-action@v1.2.5`로 접속해 host의 `/opt/mapleland/update-api.sh`를 인자 없이 실행한다.
- `host-preflight`, repository checksum, immutable digest 전달, image metadata/FCM permission gate, 별도 Environment 승인과 native OpenSSH fingerprint 검증은 사용하지 않는다.
- 새 immutable runner 구현과 전용 CI test는 저장소에서 제거했다. 초기 read-only 확인에서 active `/opt/mapleland/update-api.sh`가 legacy no-arg script이고 SHA-256이 `76650cef0cac9edf426bbc67203f4a967bb10a7b353128c6ac53aba65cc63b77`임을 확인했다.
- CI key의 forced command를 `command="/opt/mapleland/update-api.sh"`에서 `command="/usr/bin/sudo -n /opt/mapleland/update-api.sh"`로 원자 교체했다. Backup은 `/home/ubuntu/.ssh/authorized_keys.before-ci-sudo-20260716T235214Z`이며 active와 backup 모두 `ubuntu:ubuntu 0600`이다. Script는 `root:root 0755`, `.env`는 `root:root 0600`을 유지한다.
- Host에는 기존 `ubuntu ALL=(ALL) NOPASSWD: ALL`이 있어 sudoers는 변경하지 않았다. 초기 gateway 변경 뒤 `sudo -n -l`, root의 `.env` read, script `bash -n`, authorized_keys key parsing과 forced-command exact-count를 배포 실행 없이 검증했다.
- Legacy workflow는 host script를 root로 실행했지만 base Compose만 사용해 새 image에 `MANAGEMENT_SCRAPE_TOKEN`을 전달하지 못했고 container가 crash loop에 들어갔다. 이전 image `sha256:1b9b13b75debfe76ad755f618738bd63972a270b78d03f90d50133ee277fa3af` rollback도 당시 container에만 있던 Loki 설정이 재생성 과정에서 사라져 `URI with undefined scheme`으로 실패했다.
- 실패 image `sha256:3ee144df80b5110e4d72b723f570a53908f14cf9c036ae2fecb11cbbdba9569f`는 `failed-20260717T001115Z-3ee144df80b5` 태그로 보존했다. Owner 승인 복구에서 같은 image를 base와 `/opt/mapleland/docker-compose.observability.yml` 조합으로 재생성했고 exact image, `running`, restart count `0`, 공개 `/api/v1/jobs` 200과 Bearer 인증 `/actuator/prometheus` 200을 확인했다.
- Owner 승인 재발 방지 maintenance로 `/opt/mapleland/update-api.sh`의 pull/up 두 명령이 base와 observability override를 함께 사용하도록 변경했다. Root-only backup은 `/root/update-api.sh.before-observability-override-20260717T001948Z`, old SHA-256은 `76650cef0cac9edf426bbc67203f4a967bb10a7b353128c6ac53aba65cc63b77`, active SHA-256은 `436dae8156ec7115f823589aeb46b586b15e3259443bf9b1786d61fcec331b4d`다. Active는 `root:root 0755`이고 `bash -n`과 Compose render를 통과했으며 변경 후 script 자체는 실행하지 않았다.
- Host `.env`에는 `RECOMMENDATION_V1_ENGINE=AURA`, `RECOMMENDATION_V2_ENABLED=true`, `RECOMMENDATION_QUERY_TIMEOUT_SECONDS=10`이 있었지만 active override가 세 변수를 container에 전달하지 않아 v2가 code default `false`로 계속 503을 반환했다. Owner 승인 maintenance로 세 변수의 명시적 pass-through를 추가했다.
- 변경 전 override의 root-only backup은 `/root/docker-compose.observability.yml.before-recommendation-env-20260717T0115Z`이고 active override SHA-256은 `797e52dba26871e247d56be67aed34d4acd5ec26d92c184f49a3a9f9bd72a73e`다. Base와 candidate override를 실제 `.env`로 render한 뒤 `root:root 0640`으로 설치했다.
- Image pull 없이 API container만 exact image `sha256:3ee144df80b5110e4d72b723f570a53908f14cf9c036ae2fecb11cbbdba9569f`로 재생성했다. Container env의 세 설정값, `running`, restart count `0`, v1 recommendation 200, Bearer 인증 management scrape 200과 v2 recommendation 200을 확인했다. v2 smoke는 기본 limit 5에 따라 evidence score와 reasons를 가진 5개 item을 반환했다.
- 현재 v1은 Aura, v2는 MySQL, query timeout은 10초다. Schema/`EXPLAIN`, topology·SELECT 권한, reverse-proxy rate-limit과 Hikari saturation gate는 문서상 미완료이므로 v2 공개 활성화의 잔여 운영 위험으로 유지한다. 이번 maintenance에서는 v1 MySQL 전환, image pull, workflow dispatch와 merge를 수행하지 않았다.
- Legacy workflow에는 자동 복구 계약이 없다. 다음 dispatch의 수동 rollback baseline은 현재 정상 image `sha256:3ee144df80b5110e4d72b723f570a53908f14cf9c036ae2fecb11cbbdba9569f`이며 보존 tag와 base+observability override `--pull never --force-recreate` 절차를 사용한다. 다음 배포 전에도 exact current image와 tag 존재를 다시 확인한다.
- 이 복원은 검증된 운영 단순성을 우선한 명시적 위험 수용이다. `latest-arm64`의 가변성, third-party SSH action, workflow 내부 owner approval 부재, host script의 저장소 밖 drift는 잔여 위험이다. Concurrency는 겹치는 workflow run만 직렬화하며 mutable tag 자체를 immutable하게 만들지 않는다.
- 실패한 [deploy run 29540196835](https://github.com/Team-Maple/MLS-BE/actions/runs/29540196835)는 deploy 전에 추가 image 검증 script의 Bash `case` 문법 오류로 끝났고 host 변경은 시작되지 않았다.
- Merge와 운영 workflow dispatch는 owner의 별도 명시적 승인 전에는 수행하지 않는다.

## 폐기된 2026-07-17 immutable 배포 단순화 계획

아래 계획은 구현됐지만 운영 적용 전에 폐기됐다. 현재 배포 절차로 사용하지 않는다.

- 추천 branch의 [deploy run 29531165131](https://github.com/Team-Maple/MLS-BE/actions/runs/29531165131)은 host의 기존 `preflight-host.sh` checksum과 branch의 변경본이 달라 `host-preflight`에서 실패했다. Build, image publish, container pull/recreate와 운영 설정 변경은 시작되지 않았다.
- 파일 checksum을 확인하는 host script 자체가 새 checksum 계약을 알아야 하는 순환 bootstrap이 원인이었다. 애플리케이션 상태나 추천 코드 문제가 아니다.
- PR #35에서는 routine workflow를 `build-and-publish -> production approval -> deploy <immutable digest>`로 줄이고, `preflight-host.sh`와 세 checksum 전달을 제거한다. Main-only `production-build` job은 image를 publish하되 deployment record를 만들지 않고, 별도 최소권한 deploy job 하나만 `production` 승인을 받는다. Runner에는 digest allowlist, 동시 배포 lock, exact previous-image 보존, bounded public·management smoke, 자동 rollback과 root-only 실패 진단만 유지한다.
- Build가 publish tag를 한 번 digest로 해소한 뒤 FCM permission, OCI revision, run-image와 deploy output을 같은 digest로 검증하도록 바꿨다. Gradle 8.13 distribution, arm64 build runner의 pack 0.40.0 archive와 x64 deploy runner의 Tailscale 1.94.2 archive는 architecture별 공식 SHA-256으로 고정했다. Production SSH는 release binary를 다시 내려받는 third-party action 대신 runner 기본 OpenSSH와 reviewed host-key fingerprint를 사용한다.
- 당시 host의 forced-command runner는 이전 command 문법을 사용했고 active Compose override에는 recommendation 환경 전달이 없었다. 이 immutable 전환 계획은 폐기됐으며, 이후 owner 승인 legacy maintenance에서 forced command, host script와 active override를 별도로 수정했다.
- Workflow가 읽는 `FIREBASE_KEY`, `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET`, `ORACLE_SSH_KEY`는 현재 repository secret이고 Environment secret은 비어 있다. 첫 실행 전에 owner가 Firebase key를 새 `production-build`, Tailscale/SSH credential을 `production` scope로 재발급·이전하고 repository 사본을 제거해야 한다. 이 secret 변경은 아직 수행하지 않았다.
- `production` Environment variable `ORACLE_SSH_FINGERPRINT`도 아직 없다. Owner가 host console에서 확인한 SHA256 host-key fingerprint를 등록하기 전에는 deploy job이 실패하도록 고정했다.
- 삭제되는 legacy EC2 workflow가 참조하던 `HOST`, `USERNAME`, `KEY`, `PORT`, `GHCR_TOKEN`, `GHCR_USERNAME` repository secret도 남아 있다. 다른 consumer가 없는지 확인한 뒤 credential revoke/rotation과 secret 제거가 필요하며 아직 수행하지 않았다.
- `production` Environment는 owner required reviewer와 self-review 허용 상태지만 admin bypass가 가능하고 branch policy에 오래된 `feature/observability-phase-1`가 남아 있다. `production-build` Environment도 아직 없다. 첫 실행 전에 owner 승인으로 두 Environment를 `main`만 허용하고 production admin bypass를 비활성화해야 한다. 이 설정 변경은 아직 수행하지 않았다.
- 기존 mutable `:latest` EC2 workflow는 2026-04-07 이후 실행 이력이 없고 OCI 승인·rollback 경계를 우회하므로 PR #35에서 제거한다. Merge 전까지 GitHub 기본 branch에는 그대로 존재한다.
- Legacy workflow의 최근 10회 이력은 2026-01-27~2026-04-07이고 마지막 run 24086529960은 성공이다. Public API DNS는 Cloudflare proxy 주소만 보여 EC2 origin/DR 의존성 부재를 입증하지 못했다. Owner의 DNS/LB/failover 운영 inventory 확인 전에는 workflow 삭제와 legacy credential 폐기를 승인 완료로 보지 않는다.
- `production` Environment의 현재 branch policy에는 recommendation branch가 없다. Merge·runner 전환·운영 배포는 각각 owner의 명시적 승인 전에는 수행하지 않는다.
- 기존 Firebase key는 build 과정에서 bootJar/image layer에 포함돼 GHCR read 권한자가 읽을 수 있는 잔여 위험이 있다. 이번 배포 단순화 범위에서 runtime secret mount로 바꾸지 않았으며, 첫 배포 전에 owner risk acceptance 또는 별도 mount 전환·key rotation이 필요하다.

## 2026-07-16 추천 dashboard 사전 갱신

- 구현 작업은 [Issue #34](https://github.com/Team-Maple/MLS-BE/issues/34), [draft PR #35](https://github.com/Team-Maple/MLS-BE/pull/35)의 branch `feature/mysql-evidence-recommendations`에 있다. 구현/계약/관측 커밋은 각각 `79d2dff`, `037ad30`, `49bec1e`이며 애플리케이션 merge·배포와 v1 MySQL 전환은 수행하지 않았다.
- 기존 [Mapleland / Production Overview](https://mungmnb777.grafana.net/d/mapleland-production-overview/mapleland-production-overview?orgId=1&from=now-6h&to=now&timezone=browser&var-instance=mapleland-oci-1&refresh=1m)를 고정 UID `mapleland-production-overview`로 먼저 읽은 뒤 같은 UID를 `Import (Overwrite)`해 create-or-update했다. 기존 `Dashboards` folder와 datasource를 유지했다.
- live dashboard는 version `3`, panel `24`개다. version history에서 version 3이 `2026-07-16 23:21:43 KST`의 Latest이고 직전 version 2와 version 1은 restore 가능한 상태임을 확인했다.
- recommendation row와 `Recommendation request rate`, `Recommendation p95 latency`, `Error / empty / unavailable rate`, `Engine state (observed)`, `Average result count` panel, 그리고 기존 log panel을 구분하는 `Application logs` row가 각각 한 번 렌더링됨을 확인했다.
- request rate와 HTTP error는 `http_server_requests_seconds_count`, p95는 기존 `http_server_requests_seconds_bucket`의 v1/v2 route template을 사용한다. 따라서 validation/config parsing/404/5xx를 포함한 실제 endpoint traffic을 보고 custom counter는 scorer의 empty/unavailable outcome만 보완한다. live 화면에서 panel 다섯 개가 load error 없이 렌더링됐지만 애플리케이션/Alloy 변경이 아직 배포되지 않았고 추천 route traffic 표본도 없으므로 live series 존재를 완료 조건으로 주장하지 않는다. 배포 후 v2 안전 smoke에서 data와 label을 다시 확인한다.
- alert rule, threshold, contact point, notification policy, datasource, unrelated panel은 수정하거나 삭제하지 않았다. source dashboard JSON도 version 3과 동일한 24개 panel로 갱신했다.
- mapledb index/preflight 변경은 별도 [mapleland draft PR #132](https://github.com/mungmnb777/mapleland/pull/132)의 `d59d4aa`, `d7cc047`에 있으며 production DDL은 실행하지 않았다.

## 2026-07-16 최종 운영 적용 상태

- Issue: [#32](https://github.com/Team-Maple/MLS-BE/issues/32)
- draft PR: [#33](https://github.com/Team-Maple/MLS-BE/pull/33)
- 운영 애플리케이션 commit: `949976f0fd46ff60094c54e5b3df8433a6e4de59`
- 운영 애플리케이션 CI: [29490705349](https://github.com/Team-Maple/MLS-BE/actions/runs/29490705349) `success`; Gradle regression, Alloy 공식 validation, 운영 script/preflight/deploy/rollback 계약과 Grafana JSON 검증 포함
- 운영 commit 이후 PR 변경은 이 최종 증거와 runbook의 live resource 구분을 명확히 하는 문서뿐이다. 운영 바이너리와 설정은 위 commit 상태를 유지하며 PR HEAD CI 결과는 PR #33을 source of truth로 확인한다.
- 운영 배포: [29490895778](https://github.com/Team-Maple/MLS-BE/actions/runs/29490895778) `success`; host-preflight, build-and-publish, production 승인, deploy 모두 성공
- 운영 image digest: `sha256:0560502466a7b54f2bea0596e1963f68197e1bb085be42d99cd17ae959409875`; 실행 container RepoDigest와 일치
- 이전 image ID `sha256:1b9b13b75debfe76ad755f618738bd63972a270b78d03f90d50133ee277fa3af`는 rollback tag `rollback-20260716T105725955628686-1b9b13b75deb-351732`로 보존했다. 최종 배포에서 rollback은 실행되지 않았다.
- container는 `running`, restart count `0`, `SERVICE_VERSION`은 운영 commit과 일치한다. Recreate부터 started까지 2.454초, readiness까지 21.680초였고 외부 체감 중단 시간은 별도로 계측하지 않았다.
- 공개 API `8080`은 기존 `0.0.0.0/[::]` binding을 유지했다. management `18080`과 Alloy `12345`는 정확히 `127.0.0.1`에만 bind했다. firewall과 공개 port는 변경하지 않았다.
- 공개 `/api/v1/jobs`는 200이다. 공개 port의 `/actuator/prometheus`, `/actuator/health`, `/actuator/env`, `/actuator/configprops`는 모두 404다. Management Prometheus는 인증 없이는 401, root-only token을 stdin curl config로 제공하면 200이며 JVM, HTTP, HikariCP, process metric이 존재한다.
- 운영 app container의 `GRAFANA_CLOUD_*` 환경변수는 0개다. Grafana write credential은 root-only Alloy environment file에만 있고 명령, CI, Issue, PR에 출력하지 않았다.
- Alloy v1.17.1은 `active/enabled/ready`다. 30분 gate에서 prod active series 236, 배포 직전 복원 시점 대비 증가 127, RSS 최대 198,254,592 bytes, CPU 최대 0.005583 core다.
- Loki 전송량의 보수적인 5분 rate 30분 최대 일일 환산은 410,041.71 bytes/day이고 15분 rate 확인값은 168,274.29 bytes/day다. dropped entries/bytes와 ECS parse failure는 모두 0이다.
- 배포 직후 로컬 ECS 파일 42/42줄이 각각 유효한 단일-line JSON이었다. 필수 ECS field와 service 값 누락은 0이고 원본 member ID, 인증정보, secret field, email 패턴은 0이다. `0.0.0.0`은 Tomcat thread 이름에서만 탐지됐으며 사용자 IP가 아니다.
- Alloy를 2초 중단한 동안 애플리케이션과 로컬 파일 기록은 계속됐고 public API는 200이었다. 재시작 뒤 app 1줄과 fixture 1줄만 이어서 읽어 기존 42줄을 replay하지 않았다. rename/create rotation 전후 marker는 Loki에서 각각 정확히 1건, structured metadata `event_action` 집계는 2건이었다. Drop은 0이며 fixture는 검증 뒤 제거했다.
- 애플리케이션에 예외를 유발하지 않는 명시적 `observability.stack-trace-test` ECS fixture 1건으로 `error.type`, `error.message`, `error.stack_trace`, `event.action/outcome`을 검증했다. Loki는 정확히 한 로그 이벤트로 표시했고 stack trace newline은 JSON 문자열 안에 escape됐다. Fixture는 검증 뒤 제거했다.
- 대표 API 30회는 200 30/30, 평균 5.820ms, p95 7.363ms, 최대 17.869ms였다. 기존 p95 3.494ms 대비 증가는 3.869ms로 승인 기준 `max(20%, 5ms)` 이내다.
- Dashboard: [Mapleland / Production Overview](https://mungmnb777.grafana.net/d/mapleland-production-overview/mapleland-production-overview?orgId=1&from=now-6h&to=now&timezone=browser&var-instance=mapleland-oci-1&refresh=1m). 17개 핵심 panel이 실제 app/host metric과 Loki data로 렌더링되고 dashboard에서 production log Explore로 이동한다.
- Loki indexed label은 `service_name`, `deployment_environment`, `level`, `cloud_provider` 네 개뿐이다. `logger`, `thread`, event/error/HTTP/mapleland field는 structured metadata 또는 JSON field로 조회한다.
- Grafana folder는 title `Mapleland`, 실제 UID `fnp4gz`다. Versioned file-provisioning source의 논리 UID와 Cloud UI가 생성한 live UID가 다르므로 source wrapper를 Cloud API에 그대로 POST하거나 중복 생성하지 않는다.
- Alert는 [application scrape down](https://mungmnb777.grafana.net/alerting/grafana/ffsa03ecs8z5sd/view), [traffic-aware 5xx increase](https://mungmnb777.grafana.net/alerting/grafana/dfsa0kel8tfk0e/view), [host disk below 10%](https://mungmnb777.grafana.net/alerting/grafana/afsa0v0l7t340b/view) 세 개다. 모두 1분 group에서 반복 평가 후 `Normal`; Firing/Pending 0, Active notifications 0이다.
- 기존 `grafana-default-email` contact point와 default notification policy는 읽기만 했고 contact point, route, datasource, 기존 dashboard/alert, OAuth/MCP 연결을 수정·삭제하지 않았다.
- Grafana MCP 도구는 현재 Codex session에 주입되지 않아 기존 로그인된 Grafana UI를 fallback으로 사용했다. 새 OAuth 연결을 추가하거나 기존 연결을 삭제하지 않았다.
- 성공 뒤 rotation/stack fixture와 root-only `/var/log/mapleland-deploy/last-failure` 진단을 제거했다. Alloy ready, public API 200, drop 0을 다시 확인했다.

## 2026-07-16 롤아웃 중간 이력 (최종 상태 아님)

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
- 당시 활성 `/opt/mapleland/update-api.sh` SHA-256: `74e714fa058a6a2318b8f842de6ef7c0582da4e460446c844af22b12e43efb26` (restart-loop fail-fast와 root-only 진단 보존 포함).
- 당시 활성 `/opt/mapleland/preflight-host.sh` SHA-256: `860fab026264378684f7d02c373fa7bc8fefb0216a97ffafc866cd5c5093e413`.
- 당시 활성 `/opt/mapleland/docker-compose.observability.yml` SHA-256: `7a0a1f77815f19940b71f07921c7411fb91d12336df93046a6de3c676ef9e8c1`
- root-only rollback backup: `/root/mapleland-observability-20260716T063108Z-1466ebe`; `/root/mapleland-observability-rollback.env`와 exact previous-image tag 생성 완료
- 당시 운영 app image ID: `sha256:1b9b13b75debfe76ad755f618738bd63972a270b78d03f90d50133ee277fa3af`
- 당시 운영 app은 exact 이전 image로 자동 롤백되어 상태 `running`; 공개 `/api/v1/jobs` smoke 성공
- 당시 Alloy는 설치 완료, `active`였지만 애플리케이션 Observability image는 미배포 상태였다.
- 당시 base Compose는 `root:root 0640`, `.env`는 `root:root 0600`이며 active override, update/preflight script와 `SERVICE_VERSION` 준비만 완료됐다. 실행 중 container에는 적용되지 않았고 `18080` listener도 없어서 서비스 재시작이 발생하지 않았다.
- 당시 Grafana Cloud MCP는 기존 단일 연결 재로그인 뒤에도 Codex 세션에 도구가 노출되지 않았다. 연결을 추가·삭제하지 않고 기존 로그인 세션 UI를 fallback으로 사용했으며 Dashboard/alert와 app metrics/ECS log live 검증 전 상태였다.
- GitHub `production` Environment: 생성 완료. required reviewer는 `mungmnb777`, self-review 허용, 배포 branch는 `main`과 `feature/observability-phase-1`만 허용
- 실패 전환은 자동 롤백 완료; public port와 firewall 변경 없음

## 다음 안전 작업

1. PR #35의 최종 CI와 리뷰를 확인하되 owner의 별도 요청 전에는 merge 또는 legacy OCI workflow dispatch를 수행하지 않는다.
2. 반복됐던 Hikari connection validation warning의 발생량과 DB/server timeout·`maxLifetime` 정합성을 별도 후속 작업으로 조사한다.
3. 인증 재발급 경로의 NullPointerException 재현 조건과 null contract를 별도 후속 작업으로 조사한다.
4. 운영 traffic이 늘면 active series, Loki 일일 환산량, Alloy RSS/CPU와 alert noise를 같은 30분 gate로 다시 측정한다.

## 재개 규칙

- Routine GitHub Actions는 run `28788801815`와 같은 `build latest-arm64 -> Tailscale SSH -> /opt/mapleland/update-api.sh` 순서를 사용한다. Workflow 내부 owner approval과 immutable digest 보장은 없으므로 dispatch 자체를 owner의 명시적 승인으로 취급하고 종료 직후 공개 API와 dashboard를 수동 확인한다.
- manual OCI 작업은 1Password 앱 잠금 상태를 추측하지 않는다. 정확한 SSH alias `OracleCloud`를 사용하고 명시적 `SSH_AUTH_SOCK`으로 `ssh-add -l`을 확인한다. 서명 승인 상태가 불명확하면 TTY에서 `ssh-add -T ~/.ssh/1Password/SHA256_QoL9bUNkoXz+boL+ozfL1CHCMCaDSZWulM8S2cVMTWs.pub`를 먼저 실행해 1Password 승인을 받은 뒤 `BatchMode` SSH 연결을 검증한다. `oracle-cloud`처럼 다른 대소문자의 host를 사용해 identity 규칙을 우회하지 않는다.
- routine deploy는 GitHub Actions의 `ORACLE_SSH_KEY`를 사용한다. 1Password SSH agent는 manual host preparation과 break-glass 확인에만 사용한다.
- Grafana Cloud MCP는 한 Codex task만 사용한다. 병렬 agent를 시작한 상태에서 Grafana MCP를 초기화하거나 OAuth 재로그인을 반복하지 않는다.
- Grafana resource는 고정 UID를 먼저 조회한 뒤 create-or-update한다. dashboard, alert, datasource, contact point, notification route 또는 MCP 연결을 blind create/delete하지 않는다.
- 샌드박스 안의 네트워크·keyring 오류만으로 사용자 재로그인을 요청하지 않는다. 같은 read-only 명령을 승인된 네트워크 경계에서 한 번 재확인한 뒤 실제 인증 오류일 때만 조치한다.
