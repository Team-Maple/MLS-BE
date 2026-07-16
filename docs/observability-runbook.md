# Mapleland 운영 Observability 1차 Runbook

이 문서는 Issue [#32](https://github.com/Team-Maple/MLS-BE/issues/32)의 설계, 운영 적용, 검증, 장애 대응과 롤백 절차를 함께 관리한다. 이 단계의 대상은 Spring Boot 애플리케이션 로그·메트릭과 단일 Oracle Cloud VM 호스트 메트릭이다. Tempo, Java agent, OTLP 전환, synthetic monitoring은 포함하지 않는다.

## 진행 체크포인트와 재개 규칙

대화나 OAuth session을 운영 상태의 source of truth로 사용하지 않는다. 비밀값 없는 현재 상태, 마지막 외부 operation, exact checksum/image ID와 다음 안전 작업은 [`observability-rollout-checkpoint.md`](observability-rollout-checkpoint.md)에 기록한다. task가 재시작되면 이 파일, Issue, PR, 현재 branch HEAD와 운영 host의 read-only 상태를 대조한 뒤 재개한다. 이미 완료된 설치나 로그인부터 반복하지 않는다.

Grafana Cloud MCP는 단일 Codex task에서만 사용한다. 병렬 agent가 같은 rotating refresh token을 갱신하지 않게 하고, 시작 시 read probe가 실패하면 반복 재로그인이나 중복 connection 생성을 하지 않는다. `invalid_grant`가 확인됐을 때만 모든 병렬 client를 정리한 상태에서 한 번 재인증한다. dashboard와 alert는 아래 고정 UID를 조회한 뒤 create-or-update한다.

Manual OCI SSH는 1Password 앱의 잠금 상태를 추측하지 않는다. 명시적인 `SSH_AUTH_SOCK`에서 `ssh-add -l`로 Oracle key가 보이는지 확인하고, `BatchMode` SSH read-only command로 실제 signing과 접속을 검증한다. 이 두 검증 전에는 사용자에게 잠금 해제나 재로그인을 요청하지 않는다. Routine deploy는 GitHub Actions의 `ORACLE_SSH_KEY`를 사용하므로 1Password SSH agent를 배포 critical path에 넣지 않는다.

샌드박스 안의 `gh auth status`나 network command가 실패하면 같은 read-only command를 승인된 network 경계에서 한 번 재검증한다. 이번 작업에서는 sandbox 내부가 token invalid를 보고했지만 외부 재검증에서 keyring login과 `repo`, `workflow` scope가 정상임을 확인했다.

## 조사 결과와 선택

- 애플리케이션: Spring Boot 3.4.5, Java 21, Micrometer 1.14.6, Logback 1.5.18, HikariCP 5.1.0
- 실행: OCI VM의 Docker Compose, 애플리케이션 컨테이너 `1002:1001`, 공용 API `8080`
- 호스트: Ubuntu 22.04.5 LTS ARM64, 2 vCPU, 11 GiB RAM, 194 GiB ext4(조사 시 22% 사용), systemd
- Grafana Cloud: Prometheus datasource `grafanacloud-prom`, Loki datasource `grafanacloud-logs`
- 기존 로그: 애플리케이션의 Loki4j appender가 raw text를 직접 전송했다. 최근 7일 표본에서 Hikari validation 경고가 WARN/ERROR의 대부분을 차지했고 인증 재발급 NPE 및 원본 회원 식별자 로그가 관찰됐다.
- 저장소에는 `AGENTS.md`, Issue/PR template, CODEOWNERS, 별도 리뷰/배포 runbook이 없었다. 관련 선행 작업은 PR #26~#28이며, 이번 범위와 중복되는 열린 Issue/PR은 없었다.

[Spring Boot 3.4 structured logging](https://docs.spring.io/spring-boot/3.4/reference/features/logging.html#features.logging.structured)이 제공하는 내장 ECS encoder를 사용한다. 전체 Boot 업그레이드나 별도 encoder 의존성은 추가하지 않는다. 개발 콘솔은 기존 가독성 형식을 유지하고 `prod` 파일만 ECS JSON Lines로 기록한다. 기존 Loki4j 의존성과 `logback-spring.xml`을 제거해 원격 전송 실패가 애플리케이션 시작·요청 경로에 영향을 줄 수 없게 한다. Prometheus endpoint와 registry 구성은 [Spring Boot 3.4 Prometheus 문서](https://docs.spring.io/spring-boot/3.4/reference/actuator/metrics.html#actuator.metrics.export.prometheus)를 기준으로 한다.

## 데이터 흐름과 보안 경계

```text
public :443/:8080 -> Spring Boot API
                         |
                         +-> /workspace/logs/mapleland-api.json
                         |        (bind mount /var/log/mapleland-api)
                         |
host 127.0.0.1:18080 ----+-> /actuator/prometheus
          Bearer MANAGEMENT_SCRAPE_TOKEN
          |                         |
          +------------- Grafana Alloy
                             |-- 60s remote_write -> Grafana Cloud Metrics
                             +-- ECS file tail    -> Grafana Cloud Loki

host 127.0.0.1:12345 -> Alloy health/UI/metrics (외부 비공개)
```

컨테이너 내부 management server는 Docker가 호스트로 전달할 수 있도록 `0.0.0.0:18080`을 듣지만, Compose publish는 반드시 `127.0.0.1:18080:18080`이다. Traefik network나 방화벽에는 management port를 추가하지 않는다. 일반 프로세스 실행의 기본값은 `127.0.0.1:18080`이다. 동일 Docker network의 다른 컨테이너가 container IP로 management port에 도달할 가능성까지 차단하기 위해 `/actuator/prometheus`는 `MANAGEMENT_SCRAPE_TOKEN` Bearer 인증을 요구하고 Alloy만 같은 값을 root-only environment로 받는다.

Actuator exposure는 `health`, `info`, `prometheus`만 허용한다. `env`, `configprops` 등은 노출하지 않으며 health의 details/components는 `never`다. 공용 API port에는 Actuator handler 자체가 매핑되지 않는다.

## 데이터 계약

### 로그

운영 파일의 각 물리 line은 ECS JSON 객체 하나다. 기본 필드는 `@timestamp`, `log.level`, `log.logger`, `process.pid`, `process.thread.name`, `service.name`, `service.environment`, `service.version`, `message`이며 예외에는 `error.type`, `error.message`, `error.stack_trace`가 추가된다.

- `service.name=mapleland-api`
- `service.environment=prod`
- `service.version=${SERVICE_VERSION:-unknown}`; 배포 시 Git SHA 또는 release tag를 주입한다.
- active file: `/var/log/mapleland-api/mapleland-api.json`
- rolled file: `mapleland-api.YYYY-MM-DD.N.json`
- rotation: 10 MiB/file, 14일, 총 250 MiB 상한

Alloy의 기본 source는 active file의 정확한 경로만 tail한다. positions는 경로를 key로 사용하므로 `mapleland-api*.json`처럼 rolled file까지 glob하면 active 상태에서 이미 읽은 파일이 rename 뒤 새로운 경로로 발견되어 offset 0부터 중복 수집될 수 있다. 정상 rollover는 열린 active tailer가 처리하고, Alloy가 읽기 전에 retry 한계를 넘어 누락된 구간만 아래의 별도 recovery source로 수동 재생한다.

운영 exception 경계는 모두 `SafeExceptionLog`를 사용한다. `error.type`에는 예외 class를 남기되 `error.message`는 원문 대신 고정 문구 `Exception message redacted by logging policy`만 기록한다. `error.stack_trace`는 cause chain 최대 8단계, throwable별 최대 40 frame, 전체 최대 16 KiB로 제한하고 cycle을 감지한다. 상세 stack보다 먼저 각 cause의 type과 첫 `com.maple.api.*` frame을 bounded summary로 예약하므로 긴 outer stack이 한도를 소진해도 root cause와 핵심 애플리케이션 frame을 잃지 않는다. FCM 및 Apple/JWT 검증을 포함한 외부 연동, batch와 전역 exception handler도 `error.type`만 별도로 남기는 방식이 아니라 이 helper를 사용한다. 따라서 provider exception message를 포함한 원본 예외 메시지는 Loki로 보내지지 않으며 stack trace newline은 한 ECS JSON 문자열 안에서 escape된다.

이 정책은 `EcsStructuredLoggingTest`가 고정 redaction, cause/application frame 보존과 16 KiB 상한을 단위 수준에서 검증하고, `ProdFileLoggingForkIntegrationTest`가 실제 `prod` logging 설정을 별도 JVM에서 실행해 같은 계약과 한 exception당 한 JSON line을 검증한다.

Loki indexed label은 다음 네 개로 고정한다.

- `service_name`
- `deployment_environment`
- `level`
- `cloud_provider`

`logger`, `thread`, process/service version, event/HTTP/error/trace/request/`mapleland.*` 값은 structured metadata 또는 원본 ECS JSON에 남긴다. host, route, 사용자/요청/trace ID는 indexed label이 아니다. Alloy의 `stage.label_keep`가 이 allowlist를 강제한다.

민감정보 원칙은 원본 member ID, 닉네임, 이메일, IP, FCM token, access/refresh token, Authorization, cookie, password, body, query string, 원본 URL을 기록하지 않는 것이다. 객체 전체나 메서드 인자를 자동 직렬화하지 않는다. HTTP failure에는 method, response status와 Spring route template이 있을 때만 기록한다.

### 메트릭

Alloy scrape interval은 host/application/self 모두 60초, timeout은 10초다. Application allowlist는 다음 계열로 제한한다.

- `up`, `http_server_requests_seconds_*`
- `jvm_memory_*`, `jvm_gc_pause_*`, `jvm_threads_*`
- `process_*`, `system_cpu_*`
- `hikaricp_connections_*`

HTTP latency bucket은 50 ms, 100 ms, 250 ms, 500 ms, 1 s, 2 s, 5 s, 10 s의 고정 SLO 경계만 추가한다. URI는 Spring MVC route template tag를 그대로 사용하며 raw URI/query/user tag는 추가하지 않는다.

Host collector는 CPU, load, memory, filesystem, 최소 network, uname만 사용한다. Docker/loopback/VPN의 임시 network와 pseudo/container filesystem을 제외하고, remote write 전에 dashboard에 필요한 metric name allowlist를 다시 적용한다. 단일 고정 `instance=mapleland-oci-1`은 향후 host별 구분을 위한 bounded label이다.

## Grafana Alloy 고정 설치

고정 버전은 Grafana Alloy `v1.17.1`, Debian package `1.17.1-1`이다. [공식 release](https://github.com/grafana/alloy/releases/tag/v1.17.1), [Linux 설치](https://grafana.com/docs/alloy/latest/set-up/install/linux/), [Linux 권한](https://grafana.com/docs/alloy/latest/access_permissions/linux/) 문서를 기준으로 한다.

```bash
test "$(dpkg --print-architecture)" = arm64
cd /tmp
curl -fL -o alloy-1.17.1-1.arm64.deb \
  https://github.com/grafana/alloy/releases/download/v1.17.1/alloy-1.17.1-1.arm64.deb
printf '%s  %s\n' \
  '06100591d009a7d0fb594e6b248b7f82205f7c66a26cbe466641cb331b2798dd' \
  'alloy-1.17.1-1.arm64.deb' | sha256sum -c -
sudo install -d -o root -g root -m 0750 /var/cache/alloy
sudo install -o root -g root -m 0640 alloy-1.17.1-1.arm64.deb \
  /var/cache/alloy/alloy-1.17.1-1.arm64.deb
printf '%s  %s\n' \
  '06100591d009a7d0fb594e6b248b7f82205f7c66a26cbe466641cb331b2798dd' \
  '/var/cache/alloy/alloy-1.17.1-1.arm64.deb' |
  sudo sha256sum -c -
```

첫 설치와 upgrade의 package 동작을 구분한다. 첫 설치는 unit을 mask한 상태로 package/config/env를 준비한 뒤 validate하고 unmask한다. 운영 호스트에서 확인한 upgrade 결과는, 의도적으로 stop한 기존 unit이 package 설치 뒤에도 **stopped-unmasked** 상태로 남는 것이다. Upgrade/downgrade package 작업은 mask하지 않고, 이 상태를 설치된 binary의 validation gate로 사용한다. Package가 서비스를 시작해 줄 것이라고 가정하지 않으며, 설치된 binary와 최종 config를 검증하기 전에는 운영자가 시작하지 않는다.

첫 설치:

```bash
sudo systemctl mask alloy.service
sudo apt-get update
sudo apt-get install -y acl /var/cache/alloy/alloy-1.17.1-1.arm64.deb
test "$(systemctl is-enabled alloy.service || true)" = masked
```

현재 `v1.16.1` 호스트의 재현 가능한 upgrade 절차는 다음과 같다. 먼저 이전 package와 config/env/drop-in을 root-only 위치에 준비하고, rollback artifact도 실제로 checksum을 통과해야만 진행한다. 이전 ARM64 package checksum은 `6d0260aa9acd162c127fcc7448d6d25ff5b9bc1c7a85120e567cf9cbb5910b71`이다.

```bash
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_dir="/root/alloy-pre-1.17.1-${stamp}"
sudo install -d -o root -g root -m 0700 "$backup_dir"
sudo cp -a /etc/alloy/config.alloy /etc/alloy/alloy.env \
  /etc/systemd/system/alloy.service.d/override.conf "$backup_dir/"
sudo install -o root -g root -m 0640 alloy-1.16.1-1.arm64.deb \
  /var/cache/alloy/alloy-1.16.1-1.arm64.deb
printf '%s  %s\n' \
  '6d0260aa9acd162c127fcc7448d6d25ff5b9bc1c7a85120e567cf9cbb5910b71' \
  '/var/cache/alloy/alloy-1.16.1-1.arm64.deb' |
  sudo sha256sum -c -

# Stage and validate the exact new binary/config while v1.16.1 is still active.
sudo install -d -o root -g root -m 0755 "/tmp/alloy-${stamp}"
sudo dpkg-deb -x /var/cache/alloy/alloy-1.17.1-1.arm64.deb \
  "/tmp/alloy-${stamp}"
candidate_dir="/run/alloy-upgrade-${stamp}"
sudo install -d -o root -g alloy -m 0750 "$candidate_dir"
sudo install -o root -g alloy -m 0640 config.alloy \
  "$candidate_dir/config.alloy"
sudo install -o root -g root -m 0644 override.conf \
  "$candidate_dir/override.conf"
sudo systemd-run --quiet --wait --pipe --collect \
  --unit="alloy-candidate-validate-${stamp}" \
  --property=User=alloy \
  --property=Group=alloy \
  --property=EnvironmentFile=/etc/alloy/alloy.env \
  "/tmp/alloy-${stamp}/usr/bin/alloy" validate \
  "$candidate_dir/config.alloy"
```

Upgrade stop 전에 exact-path tail의 rollover 위험을 fail-closed로 확인한다. Logback active file 상한은 10 MiB이고, 최대 계획 중단은 300초다. Active file이 없으면 아직 ECS app가 배포되지 않은 상태이므로 이 검사를 건너뛸 수 있다. 파일이 있으면 60초 증가량과 1 MiB safety margin으로 stop window 동안의 headroom을 계산한다. Inode가 바뀌거나 headroom이 부족하면 Alloy가 active인 상태에서 자연 rollover가 끝날 때까지 기다린 후 처음부터 다시 측정한다. 운영자가 active file을 직접 rotate하면 안 된다.

```bash
active_log=/var/log/mapleland-api/mapleland-api.json
max_file_bytes=$((10 * 1024 * 1024))
max_stop_seconds=300
pre_stop_inode=absent
pre_stop_size=0

if sudo test -f "$active_log"; then
  inode_1="$(sudo stat -c %i "$active_log")"
  size_1="$(sudo stat -c %s "$active_log")"
  sleep 60
  inode_2="$(sudo stat -c %i "$active_log")"
  size_2="$(sudo stat -c %s "$active_log")"
  test "$inode_1" = "$inode_2"
  test "$size_2" -ge "$size_1"

  growth_60s=$((size_2 - size_1))
  projected_growth=$(((growth_60s * max_stop_seconds + 59) / 60 + 1024 * 1024))
  headroom=$((max_file_bytes - size_2))
  test "$headroom" -gt "$projected_growth"
  pre_stop_inode="$inode_2"
  pre_stop_size="$size_2"
fi
```

Preflight를 통과한 뒤에만 unit을 unmasked/stopped 상태로 만들고 시간을 기록한다. 이 시점부터 ready 복구까지 300초를 넘기지 않는다.

```bash
test "$(systemctl is-active alloy.service)" = active
sudo systemctl unmask alloy.service
upgrade_stop_epoch="$(date +%s)"
upgrade_stop_from="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
sudo systemctl stop alloy.service
if systemctl is-active --quiet alloy.service; then
  echo 'Alloy did not stop' >&2
  exit 1
fi
test "$(systemctl is-enabled alloy.service || true)" != masked
```

Package 설치 후에도 service가 stopped-unmasked인지 확인하고, 그 다음에만 최종 config/drop-in과 권한을 설치한다. Package가 예상과 달리 시작됐다면 즉시 stop하고 원인을 조사하며 validation 전에는 다시 시작하지 않는다.

```bash
sudo apt-get install -y /var/cache/alloy/alloy-1.17.1-1.arm64.deb
dpkg-query -W -f='${db:Status-Abbrev}\n' alloy | grep -q '^ii'
test "$(dpkg-query -W -f='${Version}' alloy)" = '1.17.1-1'
if systemctl is-active --quiet alloy.service; then
  sudo systemctl stop alloy.service
  echo 'package unexpectedly started Alloy; inspect maintainer-script output' >&2
  exit 1
fi
test "$(systemctl is-enabled alloy.service || true)" != masked
```

패키지가 편의를 위해 만든 `adm`, `systemd-journal` membership은 이 pipeline에 필요하지 않다. `/etc/alloy`는 package 설치 뒤 다시 `0770`이 될 수 있으므로 모든 package 작업이 끝난 뒤에도 `0750`을 재강제한다. source file은 안전한 staging directory에 준비돼 있어야 한다.

```bash
sudo gpasswd --delete alloy adm 2>/dev/null || true
sudo gpasswd --delete alloy systemd-journal 2>/dev/null || true
if id -nG alloy | grep -Eq '(^| )(adm|systemd-journal)( |$)'; then
  echo 'unexpected Alloy supplementary log group' >&2
  exit 1
fi

config_source="${candidate_dir:-.}/config.alloy"
override_source="${candidate_dir:-.}/override.conf"
sudo install -d -o root -g alloy -m 0750 /etc/alloy
sudo install -o root -g alloy -m 0640 "$config_source" \
  /etc/alloy/config.alloy
sudo install -d -o root -g root -m 0755 /etc/systemd/system/alloy.service.d
sudo install -o root -g root -m 0644 "$override_source" \
  /etc/systemd/system/alloy.service.d/override.conf
sudo install -d -o 1002 -g 1001 -m 0750 /var/log/mapleland-api
command -v setfacl
command -v getfacl
command -v setpriv
sudo setfacl -R -m u:alloy:rX /var/log/mapleland-api
sudo find /var/log/mapleland-api -type d \
  -exec setfacl -m d:u:alloy:r-x {} +
sudo install -d -o root -g alloy -m 0750 /var/lib/alloy/recovery
sudo install -d -o root -g alloy -m 0750 \
  /var/lib/alloy/recovery/quarantine
```

`/etc/alloy/alloy.env`는 첫 설치 때 `alloy.env.example`에서 만들고, upgrade 때는 기존 파일을 보존한다. Grafana token은 각각 `metrics:write`, `logs:write` 최소 scope access policy를 사용한다. `MANAGEMENT_SCRAPE_TOKEN`은 충분히 긴 random 값으로 만들고 애플리케이션의 root-only `/opt/mapleland/.env`에도 같은 값을 주입한다. 어느 값도 shell history, stdout, CI log, Issue/PR에 출력하지 않는다.

```bash
if ! sudo test -e /etc/alloy/alloy.env; then
  sudo install -o root -g root -m 0600 alloy.env /etc/alloy/alloy.env
fi
sudo chown root:root /etc/alloy/alloy.env /opt/mapleland/.env
sudo chmod 0600 /etc/alloy/alloy.env /opt/mapleland/.env
sudo -u alloy test -r /etc/alloy/config.alloy
sudo -u alloy test -x /var/log/mapleland-api

# Prove that package work didn't restore broad supplementary log access.
if id -nG alloy | grep -Eq '(^| )(adm|systemd-journal)( |$)'; then
  echo 'unexpected Alloy supplementary log group after package install' >&2
  exit 1
fi
test "$(sudo stat -c '%U:%G %a' /etc/alloy)" = 'root:alloy 750'
test "$(sudo stat -c '%U:%G %a' /etc/alloy/config.alloy)" = 'root:alloy 640'
test "$(sudo stat -c '%U:%G %a' /etc/alloy/alloy.env)" = 'root:root 600'
sudo getfacl -cp /var/log/mapleland-api |
  grep -Fx 'default:user:alloy:r-x'

# Prove default-ACL inheritance using the actual application UID/GID.
acl_probe="/var/log/mapleland-api/.alloy-acl-probe-${stamp:-initial}"
sudo setpriv --reuid=1002 --regid=1001 --clear-groups \
  sh -c 'umask 0027; : > "$1"' sh "$acl_probe"
sudo -u alloy test -r "$acl_probe"
sudo rm -f "$acl_probe"
```

Upgrade에서는 위 package 이후 hardening과 ACL probe가 성공한 뒤 설치된 v1.17.1 binary를 검증한다. Service는 이 validation이 성공할 때까지 stopped-unmasked 상태여야 한다.

```bash
sudo systemctl daemon-reload
if systemctl is-active --quiet alloy.service; then
  echo 'Alloy must remain stopped until installed-binary validation' >&2
  exit 1
fi
test "$(systemctl is-enabled alloy.service || true)" != masked
sudo systemd-run --quiet --wait --pipe --collect \
  --unit="alloy-config-validate-final-${stamp}" \
  --property=User=alloy \
  --property=Group=alloy \
  --property=EnvironmentFile=/etc/alloy/alloy.env \
  /usr/bin/alloy validate /etc/alloy/config.alloy

sudo systemctl enable alloy.service
sudo systemctl start alloy.service
test "$(systemctl is-enabled alloy.service)" = enabled
test "$(systemctl is-active alloy.service)" = active
curl --fail --silent http://127.0.0.1:12345/-/ready
upgrade_ready_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
upgrade_stop_seconds=$(($(date +%s) - upgrade_stop_epoch))
test "$upgrade_stop_seconds" -le "$max_stop_seconds"
```

Active file이 있었던 upgrade는 ready 직후 inode/size를 다시 확인한다. Inode가 바뀌었거나 size가 작아졌으면 stop window 중 rollover된 것이므로 완료로 처리하지 않는다. 기록한 `[upgrade_stop_from, upgrade_ready_at]` 범위를 아래 quarantine recovery 절차로 즉시 대조·복구한다. Inode가 그대로면 positions resume과 exact-message 중복 부재를 확인한다.

```bash
if test "$pre_stop_inode" != absent; then
  post_start_inode="$(sudo stat -c %i "$active_log")"
  post_start_size="$(sudo stat -c %s "$active_log")"
  if test "$post_start_inode" != "$pre_stop_inode" ||
     test "$post_start_size" -lt "$pre_stop_size"; then
    echo "rollover detected; recover ${upgrade_stop_from}..${upgrade_ready_at}" >&2
    exit 1
  fi
fi
```

첫 설치에서는 공통 권한/config/env 단계와 설치된 binary validation을 마친 다음 아래처럼 시작한다.

```bash
sudo systemd-run --quiet --wait --pipe --collect \
  --unit="alloy-config-validate-${stamp:-initial}" \
  --property=User=alloy \
  --property=Group=alloy \
  --property=EnvironmentFile=/etc/alloy/alloy.env \
  /usr/bin/alloy validate /etc/alloy/config.alloy
sudo systemctl unmask alloy.service
sudo systemctl daemon-reload
sudo systemctl enable --now alloy.service
```

Upgrade/첫 설치 모두 다음 경계를 확인한다.

```bash
test "$(systemctl is-active alloy.service)" = active
curl --fail --silent http://127.0.0.1:12345/-/ready
alloy_sockets="$(sudo ss -H -lnt 'sport = :12345')"
printf '%s\n' "$alloy_sockets" |
  awk '$4 == "127.0.0.1:12345" { exact++ }
       END { exit !(NR == 1 && exact == 1) }'
sudo stat -c '%U:%G %a %n' /etc/alloy /etc/alloy/config.alloy \
  /etc/alloy/alloy.env /var/lib/alloy
```

Upgrade validation 또는 readiness가 실패하면 exact 이전 deb의 checksum을 다시 확인하고 rollback한다. 순서는 **restored config를 extracted 이전 binary로 validate → unit이 unmasked/stopped인지 확인 → package downgrade → package 이후 hardening/ACL probe → 설치된 이전 binary validate → explicit start/ready**다. Package install 중에는 mask하지 않는다.

```bash
sudo systemctl stop alloy.service
sudo systemctl unmask alloy.service
if systemctl is-active --quiet alloy.service; then
  echo 'Alloy did not stop for rollback' >&2
  exit 1
fi
test "$(systemctl is-enabled alloy.service || true)" != masked
printf '%s  %s\n' \
  '6d0260aa9acd162c127fcc7448d6d25ff5b9bc1c7a85120e567cf9cbb5910b71' \
  '/var/cache/alloy/alloy-1.16.1-1.arm64.deb' |
  sudo sha256sum -c -
sudo install -o root -g alloy -m 0640 "$backup_dir/config.alloy" \
  /etc/alloy/config.alloy
sudo install -o root -g root -m 0600 "$backup_dir/alloy.env" \
  /etc/alloy/alloy.env
sudo install -o root -g root -m 0644 "$backup_dir/override.conf" \
  /etc/systemd/system/alloy.service.d/override.conf
sudo systemctl daemon-reload

rollback_extract="/run/alloy-rollback-${stamp}"
sudo install -d -o root -g alloy -m 0750 "$rollback_extract"
sudo dpkg-deb -x /var/cache/alloy/alloy-1.16.1-1.arm64.deb \
  "$rollback_extract"
sudo systemd-run --quiet --wait --pipe --collect \
  --unit="alloy-rollback-candidate-validate-${stamp}" \
  --property=User=alloy \
  --property=Group=alloy \
  --property=EnvironmentFile=/etc/alloy/alloy.env \
  "$rollback_extract/usr/bin/alloy" validate /etc/alloy/config.alloy

sudo apt-get install -y --allow-downgrades \
  /var/cache/alloy/alloy-1.16.1-1.arm64.deb
test "$(dpkg-query -W -f='${Version}' alloy)" = '1.16.1-1'
if systemctl is-active --quiet alloy.service; then
  sudo systemctl stop alloy.service
  echo 'downgrade unexpectedly started Alloy; inspect package output' >&2
  exit 1
fi
test "$(systemctl is-enabled alloy.service || true)" != masked

# Reinstall the backed-up files in case package conffile handling changed them.
sudo install -d -o root -g alloy -m 0750 /etc/alloy
sudo install -o root -g alloy -m 0640 "$backup_dir/config.alloy" \
  /etc/alloy/config.alloy
sudo install -o root -g root -m 0600 "$backup_dir/alloy.env" \
  /etc/alloy/alloy.env
sudo install -o root -g root -m 0644 "$backup_dir/override.conf" \
  /etc/systemd/system/alloy.service.d/override.conf
sudo gpasswd --delete alloy adm 2>/dev/null || true
sudo gpasswd --delete alloy systemd-journal 2>/dev/null || true
if id -nG alloy | grep -Eq '(^| )(adm|systemd-journal)( |$)'; then
  echo 'unexpected Alloy supplementary log group after downgrade' >&2
  exit 1
fi
sudo setfacl -R -m u:alloy:rX /var/log/mapleland-api
sudo find /var/log/mapleland-api -type d \
  -exec setfacl -m d:u:alloy:r-x {} +
rollback_acl_probe="/var/log/mapleland-api/.alloy-acl-probe-rollback-${stamp}"
sudo setpriv --reuid=1002 --regid=1001 --clear-groups \
  sh -c 'umask 0027; : > "$1"' sh "$rollback_acl_probe"
sudo -u alloy test -r "$rollback_acl_probe"
sudo rm -f "$rollback_acl_probe"

sudo systemctl daemon-reload
sudo systemd-run --quiet --wait --pipe --collect \
  --unit="alloy-rollback-installed-validate-${stamp}" \
  --property=User=alloy \
  --property=Group=alloy \
  --property=EnvironmentFile=/etc/alloy/alloy.env \
  /usr/bin/alloy validate /etc/alloy/config.alloy
sudo systemctl enable alloy.service
sudo systemctl start alloy.service
test "$(systemctl is-enabled alloy.service)" = enabled
test "$(systemctl is-active alloy.service)" = active
curl --fail --silent http://127.0.0.1:12345/-/ready
rollback_ready_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
if test "${pre_stop_inode:-absent}" != absent; then
  rollback_inode="$(sudo stat -c %i "$active_log")"
  rollback_size="$(sudo stat -c %s "$active_log")"
  if test "$rollback_inode" != "$pre_stop_inode" ||
     test "$rollback_size" -lt "$pre_stop_size"; then
    echo "rollover detected; recover ${upgrade_stop_from}..${rollback_ready_at}" >&2
    exit 1
  fi
fi
```

Upgrade 중 애플리케이션 재시작, port/firewall 변경이나 Grafana resource 변경은 없다.

package unit의 storage path `/var/lib/alloy/data`를 유지하므로 rollover가 없을 때 file positions와 Prometheus remote-write WAL은 재시작 후 이어진다. Stop 중 rollover는 exact-path source가 과거 inode를 자동 발견하지 못하므로 위 preflight와 아래 quarantine recovery가 필수다. Loki write는 안정 기능 기준으로 WAL이 없고 유한 retry 뒤 drop될 수 있으므로 같은 보장을 하지 않는다. UI/health는 override의 `127.0.0.1:12345`에서만 듣는다.

```bash
systemctl is-active alloy
curl --fail --silent http://127.0.0.1:12345/-/ready
journalctl -u alloy --since '10 minutes ago' --no-pager
sudo stat -c '%U:%G %a %n' /etc/alloy /etc/alloy/config.alloy \
  /etc/alloy/alloy.env /var/lib/alloy
```

## 애플리케이션 적용 순서

이 절은 단일 컨테이너 재생성을 포함하므로 실행 직전 승인이 필요하다.

재시작 전에 현재 container image ID를 별도 local tag로 고정하고 Compose, `.env`, 기존 `update-api.sh`를 root-only 경로에 백업한다. Registry tag는 pull 뒤 다른 digest를 가리킬 수 있으므로 digest 또는 그 image ID에 붙인 rollback tag 없이 `docker compose up`만 실행하는 것은 롤백이 아니다. Build job은 run-attempt별 tag로 publish한 직후 registry manifest descriptor의 digest를 읽고 deploy job에는 `ghcr.io/team-maple/mls-be/mapleland-api@sha256:...`만 전달한다. 저장소의 `deploy/observability/update-api.sh`도 digest ref 외에는 거부하고, 매 배포 직전 image를 별도 rollback tag로 고정한 뒤 `--pull never`로 재생성한다. 새 image의 exact ID, management health와 public smoke가 90초 안에 모두 통과하지 않으면 exact 이전 image로 자동 복구하고 workflow를 실패시킨다.

```bash
cd /opt/mapleland
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
compose_backup="/root/docker-compose.yml.pre-observability-${stamp}"
env_backup="/root/mapleland.env.pre-observability-${stamp}"
update_script_backup="/root/update-api.sh.pre-observability-${stamp}"
previous_image_id="$(sudo docker inspect --format '{{.Image}}' mapleland-mapleland-api-1)"
rollback_tag="ghcr.io/team-maple/mls-be/mapleland-api:rollback-${stamp}"
image_name="ghcr.io/team-maple/mls-be/mapleland-api"
compose_image="$(sudo docker compose config --images |
  grep -E '^ghcr[.]io/team-maple/mls-be/mapleland-api:')"
test -n "$compose_image"
test "$(printf '%s\n' "$compose_image" | grep -c .)" -eq 1
expected_commit='<replace-with-full-40-character-commit-sha>'
staged_update_script='<replace-with-absolute-reviewed-staging-path>/update-api.sh'
staged_preflight_script='<replace-with-absolute-reviewed-staging-path>/preflight-host.sh'
staged_compose_override='<replace-with-absolute-reviewed-staging-path>/docker-compose.override.example.yml'
expected_update_script_sha256='<replace-with-reviewed-script-sha256>'
expected_preflight_script_sha256='<replace-with-reviewed-script-sha256>'
expected_compose_override_sha256='<replace-with-reviewed-file-sha256>'
printf '%s\n' "$expected_commit" | grep -Eq '^[0-9a-f]{40}$'
printf '%s\n' "$expected_update_script_sha256" | grep -Eq '^[0-9a-f]{64}$'
printf '%s\n' "$expected_preflight_script_sha256" | grep -Eq '^[0-9a-f]{64}$'
printf '%s\n' "$expected_compose_override_sha256" | grep -Eq '^[0-9a-f]{64}$'
sudo test -f "$staged_update_script"
sudo test -f "$staged_preflight_script"
sudo test -f "$staged_compose_override"
sudo bash -n "$staged_update_script"
sudo bash -n "$staged_preflight_script"
printf '%s  %s\n' "$expected_update_script_sha256" \
  "$staged_update_script" | sudo sha256sum -c -
printf '%s  %s\n' "$expected_preflight_script_sha256" \
  "$staged_preflight_script" | sudo sha256sum -c -
printf '%s  %s\n' "$expected_compose_override_sha256" \
  "$staged_compose_override" | sudo sha256sum -c -

sudo cp -a docker-compose.yml "$compose_backup"
sudo cp -a .env "$env_backup"
sudo cp -a /opt/mapleland/update-api.sh "$update_script_backup"
sudo docker image tag "$previous_image_id" "$rollback_tag"
sudo sh -c "umask 077; printf \
  'COMPOSE_BACKUP=%s\nENV_BACKUP=%s\nUPDATE_SCRIPT_BACKUP=%s\nROLLBACK_TAG=%s\nROLLBACK_COMPOSE_IMAGE=%s\nEXPECTED_COMMIT=%s\n' \
  '$compose_backup' '$env_backup' '$update_script_backup' \
  '$rollback_tag' '$compose_image' \
  '$expected_commit' \
  > /root/mapleland-observability-rollback.env"
sudo docker image inspect "$rollback_tag" >/dev/null
sudo install -o root -g root -m 0755 "$staged_update_script" \
  /opt/mapleland/update-api.sh
sudo install -o root -g root -m 0755 "$staged_preflight_script" \
  /opt/mapleland/preflight-host.sh
sudo install -o root -g root -m 0640 "$staged_compose_override" \
  /opt/mapleland/docker-compose.observability.yml
if ! sudo test -e /opt/mapleland/ghcr.env; then
  sudo install -o root -g root -m 0600 /dev/null /opt/mapleland/ghcr.env
fi
# Populate only GHCR_USER and a read:packages GHCR_TOKEN through the approved
# secret channel. Do not paste either value into shell history or this runbook.
sudoedit /opt/mapleland/ghcr.env
```

1. `/var/log/mapleland-api`의 owner `1002:1001`, mode와 기존/default Alloy ACL을 확인한다.
2. root-only `/opt/mapleland/.env`와 `/etc/alloy/alloy.env`에 동일한 `MANAGEMENT_SCRAPE_TOKEN`을 안전하게 입력한다. `/opt/mapleland/.env`는 `root:root 0600`으로 낮춘다.
3. 기존 Compose `.env`를 shell로 `source`하지 않는다. `ghcr.env.example`의 두 key만 포함하는 `/opt/mapleland/ghcr.env`를 별도로 만들고 `root:root 0600`, non-symlink인지 확인한다. `update-api.sh`는 이 파일을 실행하지 않고 exact key/value parser로만 읽는다.
4. `docker-compose.override.example.yml`을 checksum 검증 후 `/opt/mapleland/docker-compose.observability.yml`로 설치한다. 첫 전환 전에는 base Compose와 `.env`의 `GRAFANA_CLOUD_URL`, `GRAFANA_CLOUD_USERNAME`, `GRAFANA_CLOUD_PASSWORD` 세 legacy entry를 제거하지 않는다. 새 container가 실패하면 이전 Loki4j image가 exact rollback 입력으로 사용할 수 있어야 하기 때문이다. `update-api.sh`와 preflight는 이 세 entry를 제외한 root-only 임시 base와 `--env-file`을 사용해 새 container model을 render한다. App service의 기존 repository tag 계약은 `update-api.sh`가 digest-pinned image ID로 local retag하므로 임의 변경하지 않는다.
5. `SERVICE_VERSION`을 위 `expected_commit`으로 설정한다. Publish workflow run, commit과 build job이 확정한 manifest digest를 운영 기록에 남긴다.
6. 새 image의 readiness, management health, public smoke와 실제 container environment의 legacy key 부재가 모두 확인된 뒤에만 `update-api.sh`가 base Compose와 `.env`의 세 entry를 같은 directory의 root-only candidate로 만들고 render validation 후 atomic rename한다. 실패 전에는 원본을 보존하고, 성공 뒤에는 root-only backup에만 남긴다. Alloy write secret과 이 legacy rollback 값은 서로 다른 파일과 목적을 유지한다.
7. `18080`이 비어 있는지 다시 확인한다. public/Traefik routing이나 firewall rule은 추가하지 않는다. 저장소에서 계산한 두 script와 override checksum으로 `/opt/mapleland/preflight-host.sh`를 실행해 sanitized Compose render, legacy rollback entry의 `3:3` 또는 `0:0` 완전성, root-only env, active deployment script, log ACL, Alloy readiness/loopback listener, 현재 app container와 rollback image, public smoke를 읽기 전용으로 검증한다.
8. 승인 후 workflow를 dispatch한다. Workflow 순서는 `host-preflight -> build-and-publish -> production Environment approval -> host-preflight recheck -> deploy`다. 첫 preflight가 실패하면 image를 만들지 않으며, 두 번째 preflight와 `update-api.sh`는 forced-command gateway의 한 `deploy` 요청에서 연속 실행된다. Build job은 manifest digest를 `image_ref` output으로 넘기고 gateway가 세 checksum과 `<repository>@sha256:<digest>` 전체를 allowlist로 검증한다. Root update script가 digest pull, per-deploy rollback tag, local retag, `--pull never`, legacy environment 부재, exact image ID, bounded readiness/smoke를 모두 성공해야 한다. 첫 전환 실패 시 base Compose와 원래 `.env`로 이전 Loki4j image를 복구하고, 성공 시에만 legacy entry를 제거한다. 한 번의 전환으로 Loki4j를 제거하고 Alloy file tail을 시작해 중복 전송 구간을 만들지 않는다.
9. 실행 container의 image ID와 `SERVICE_VERSION`이 기대값과 같은지 먼저 확인하고, management health, unauthenticated scrape 거부, 공용 health/proxy, 대표 API, local ECS file, Alloy authenticated scrape, Grafana arrival를 순서대로 확인한다.

첫 전환 동안 preflight와 update script는 기본 `.env` 자동 로드를 사용하지 않고 legacy 세 entry만 제외한 mode `0640` 임시 base와 mode `0600` 임시 `--env-file`을 같은 project directory에서 사용한다. 따라서 원본 rollback 값은 보존하면서도 새 container에는 전달하지 않는다. 명시적 env file의 project environment 동작은 [Docker Compose `--env-file` 규칙](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/#local-env-file-versus-project-directory-env-file)을 따른다. 실제 container의 `.Config.Env`도 readiness gate에서 값 출력 없이 재검증한다. 성공한 후보 파일만 active base와 `.env`로 승격된다.

`sudo`는 Docker 접근을 위한 것이 아니라 root-only deployment state를 위한 것이다. Script는 mode `0600`인 `.env`/`ghcr.env`를 읽고 mode `0640`인 Compose 계약을 검증·교체하므로 EUID 0이 아니면 중단한다. Workflow가 상승시키는 범위는 checksum이 preflight에서 검증된 `/opt/mapleland/update-api.sh` 실행 한 번이며, 일반 사용자에게 secret read나 Compose write 권한을 부여하지 않는다.

GitHub CI key의 `authorized_keys` 항목은 `command="/opt/mapleland/update-api.sh"`, `no-port-forwarding`, `no-agent-forwarding`, `no-X11-forwarding`, `no-pty`를 유지한다. 따라서 SSH client가 보낸 command는 직접 실행되지 않고 OpenSSH가 강제 entrypoint의 `SSH_ORIGINAL_COMMAND`에 보존한다. Non-root entrypoint는 newline 없는 단일 line 전체를 정규식으로 검증하며 `preflight <3 checksums>` 또는 `deploy <3 checksums> <repository@sha256:digest>`만 허용한다. 표준입력, shell `eval`, tag image, 임의 command와 추가 argument는 실행 경계로 사용하지 않는다. Allowlist를 통과한 경우에만 `/usr/bin/sudo -n`으로 고정된 preflight/update script를 호출하고, root로 재진입한 update script가 실제 배포를 수행한다.

운영 Docker Compose 2.38.2는 long bind syntax의 `create_host_path: false`를 적용하면서도 `config --format json`에서는 false 기본값을 `null`로 생략한다. Preflight는 active override의 SHA-256을 reviewed source와 먼저 대조하므로 해당 source가 `create_host_path: false`임을 바이트 단위로 고정하고, rendered model에서는 bind type/source/target과 `create_host_path`가 `true`가 아님을 확인한다. `true`, 다른 source/target 또는 checksum 불일치는 모두 fail closed다.

```bash
sudo chown root:root /opt/mapleland/.env
sudo chmod 0600 /opt/mapleland/.env
sudo chown root:root /opt/mapleland/docker-compose.yml
sudo chmod 0640 /opt/mapleland/docker-compose.yml
test "$(sudo stat -c '%U:%G %a' /opt/mapleland/docker-compose.yml)" = 'root:root 640'
test "$(sudo stat -c '%U:%G %a' /opt/mapleland/docker-compose.observability.yml)" = 'root:root 640'
sudo test -f /opt/mapleland/ghcr.env
sudo test ! -L /opt/mapleland/ghcr.env
test "$(sudo stat -c '%U:%G %a' /opt/mapleland/ghcr.env)" = 'root:root 600'
test "$(sudo awk -F= '
  /^(GHCR_USER|GHCR_TOKEN)=/ { seen[$1]++ }
  END { print (seen["GHCR_USER"] == 1 && seen["GHCR_TOKEN"] == 1 && length(seen) == 2) ? "ok" : "bad" }
' /opt/mapleland/ghcr.env)" = ok
test -d /var/log/mapleland-api
if ss -lntH 'sport = :18080' | grep -q .; then
  echo 'management port 18080 is already in use' >&2
  exit 1
fi
cd /opt/mapleland
effective_images="$(sudo docker compose -f docker-compose.yml config --images)"
effective_app_image="$(printf '%s\n' "$effective_images" |
  grep -E '^ghcr[.]io/team-maple/mls-be/mapleland-api:')"
test -n "$effective_app_image"
test "$effective_app_image" = "$compose_image"
sudo /opt/mapleland/preflight-host.sh \
  "$expected_preflight_script_sha256" \
  "$expected_update_script_sha256" \
  "$expected_compose_override_sha256"
```

Preflight는 secret 값이나 rendered Compose를 출력하지 않고 non-secret checksum, current image ID와 management listener 상태로 만든 attestation만 출력한다. checksum mismatch, broad env permission, wildcard listener, Compose boundary 누락, Alloy/readiness/ACL 또는 public smoke 실패가 있으면 원인을 해결하기 전 workflow를 재실행하지 않는다. 특히 active script가 저장소와 다르면 legacy script를 실행해 보는 방식으로 확인하지 않는다.

배포 readiness는 전체 `/actuator/health` 집계를 사용하지 않는다. 이 집계는 선택적 Neo4j 같은 외부 dependency 하나가 `DOWN`이어도 503이 되어, JVM·관리 서버·핵심 MySQL API가 정상인 후보를 불필요하게 롤백할 수 있다. 대신 Bearer 인증된 Prometheus endpoint로 실제 Alloy scrape 경계를 확인하고 `/api/v1/jobs`로 공개 서버와 핵심 DB 경계를 함께 확인한다. Token은 curl config stdin으로만 전달하며 argv와 로그에 남기지 않는다.

승인 요청에는 다음 값을 채운다.

- 예상 중단: 기존 기동 17.742초를 기준으로 정상 20~40초의 단일 컨테이너 교체. readiness hard timeout은 90초이며 실패 시 이전 image 자동 복구까지 최악 약 180초
- 변경 port: 컨테이너 `18080`, 호스트 `127.0.0.1:18080`만 추가; 공개 port 변화 없음
- artifact: full commit SHA, run-attempt별 publish tag, build job이 확정한 `repository@sha256:<manifest-digest>`, 실행 전후 exact image ID
- management readiness: Bearer 인증된 `http://127.0.0.1:18080/actuator/prometheus`가 성공하고 Prometheus text를 반환
- public smoke: `curl -fsS http://127.0.0.1:8080/api/v1/jobs`
- rollback: `/root/mapleland-observability-rollback.env`에 기록된 exact local image tag와 Compose/`.env`/script backup으로 아래 명령 실행
- Grafana 확인: 아래 PromQL/LogQL 및 Production Overview panel

GitHub `production` Environment required reviewer 승인을 받은 뒤에만 deploy job이 시작된다. 아래는 workflow가 host에서 preflight를 다시 통과한 직후 호출하는 명령과 사후 검증의 등가 표현이다. `image_ref`는 build job output에서 복사한 digest ref여야 하며 tag를 입력하면 script가 거부한다.

`production` Environment는 required reviewer `mungmnb777`와 custom branch policy를 사용한다. 허용 branch는 `main`, `feature/observability-phase-1` 두 개뿐이며 이번 rollout 종료 후 feature branch policy만 제거한다. Environment 자체나 기존 deployment history는 삭제하지 않는다.

```bash
image_ref='ghcr.io/team-maple/mls-be/mapleland-api@sha256:<replace-with-64-hex-digest>'
printf '%s\n' "$image_ref" | grep -Eq \
  '^ghcr[.]io/team-maple/mls-be/mapleland-api@sha256:[0-9a-f]{64}$'
sudo /opt/mapleland/update-api.sh "$image_ref"
expected_image_id="$(sudo docker image inspect --format '{{.Id}}' "$image_ref")"
actual_image_id="$(sudo docker inspect --format '{{.Image}}' mapleland-mapleland-api-1)"
test "$actual_image_id" = "$expected_image_id"
sudo docker inspect mapleland-mapleland-api-1 |
  jq -e --arg expected "SERVICE_VERSION=${expected_commit}" \
    '.[0].Config.Env | index($expected) != null' >/dev/null
```

`update-api.sh` 내부 readiness, legacy environment 부재 또는 smoke가 실패하면 배포마다 생성한 exact previous-image tag로 먼저 자동 복구되고 workflow는 실패한다. 첫 Observability 전환에서는 성공 전까지 보존한 base Compose와 `.env`를 사용하므로 이전 Loki4j image의 환경도 함께 복구된다. 전환 완료 뒤의 배포는 sanitized env와 observability override로 직전 image를 복구한다. 자동 rollback이 public smoke를 회복하지 못하거나 이후 behavioral 검증이 실패하면 다음 full rollback을 즉시 실행한다.

```bash
sudo sh -eu -c '
  . /root/mapleland-observability-rollback.env
  cp -a "$COMPOSE_BACKUP" /opt/mapleland/docker-compose.yml
  cp -a "$ENV_BACKUP" /opt/mapleland/.env
  cp -a "$UPDATE_SCRIPT_BACKUP" /opt/mapleland/update-api.sh
  if test -f /opt/mapleland/docker-compose.observability.yml; then
    mv /opt/mapleland/docker-compose.observability.yml \
      "/root/docker-compose.observability.yml.disabled-${EXPECTED_COMMIT}"
  fi
  docker image tag "$ROLLBACK_TAG" "$ROLLBACK_COMPOSE_IMAGE"
  expected_rollback_image_id="$(docker image inspect --format "{{.Id}}" "$ROLLBACK_TAG")"
  cd /opt/mapleland
  docker compose up -d --no-deps --force-recreate --pull never mapleland-api
  actual_rollback_image_id="$(docker inspect --format "{{.Image}}" mapleland-mapleland-api-1)"
  test "$actual_rollback_image_id" = "$expected_rollback_image_id"
'
```

## 자동 검증

다음 두 test는 stack trace 정책의 서로 다른 경계를 고정한다. `EcsStructuredLoggingTest`는 `SafeExceptionLog`의 원본 exception message·식별자·인증정보 redaction, cause/root application frame 보존, 16 KiB 상한과 한 event당 한 JSON line을 검증한다. `ProdFileLoggingForkIntegrationTest`는 별도 JVM의 실제 `prod` profile과 실제 rolling file appender에서도 동일한 계약이 유지되고 모든 물리 line이 유효한 ECS JSON인지 검증한다.

```bash
./gradlew test \
  --tests 'com.maple.api.observability.EcsStructuredLoggingTest' \
  --tests 'com.maple.api.observability.ProdFileLoggingForkIntegrationTest'
```

이 targeted test와 전체 `./gradlew test`가 모두 성공한 결과를 PR의 자동 검증 근거로 남긴다. 실패한 assertion을 우회하거나 민감 문자열을 fixture 밖 운영 로그로 출력해 확인하지 않는다.

## Behavioral verification

### HTTP, 파일, metric

```bash
unauthenticated_code="$(curl -sS -o /dev/null -w '%{http_code}' \
  http://127.0.0.1:18080/actuator/prometheus)"
case "$unauthenticated_code" in
  401|403) ;;
  *) echo "unexpected unauthenticated management status: $unauthenticated_code" >&2; exit 1 ;;
esac

# Feed the header through curl config stdin so the token isn't placed in argv.
sudo sh -eu -c '
  . /etc/alloy/alloy.env
  printf "header = \"Authorization: Bearer %s\"\n" "$MANAGEMENT_SCRAPE_TOKEN" |
    curl --fail --silent --show-error --config - \
      http://127.0.0.1:18080/actuator/prometheus
' | grep -E \
  'http_server_requests_seconds_count|jvm_memory_used_bytes|hikaricp_connections_active'

for endpoint in prometheus health env configprops; do
  public_actuator_code="$(curl -sS -o /dev/null -w '%{http_code}' \
    "http://127.0.0.1:8080/actuator/${endpoint}")"
  test "$public_actuator_code" = 404
done
curl --fail --silent http://127.0.0.1:18080/actuator/health |
  jq -e '. == {"status":"UP"}' >/dev/null

# Verify Docker has one IPv4-loopback mapping and no wildcard/IPv6 mapping.
sudo docker inspect mapleland-mapleland-api-1 |
  jq -e '
    .[0].NetworkSettings.Ports["18080/tcp"] as $bindings |
    ($bindings | length) == 1 and
    $bindings[0].HostIp == "127.0.0.1" and
    $bindings[0].HostPort == "18080"
  ' >/dev/null

# The complete socket set for each management port must be exactly one IPv4
# loopback listener. A wildcard, private-address, or IPv6 listener fails.
for port in 12345 18080; do
  sockets="$(sudo ss -H -lnt "sport = :${port}")"
  printf '%s\n' "$sockets" |
    awk -v expected="127.0.0.1:${port}" \
      '$4 == expected { exact++ }
       END { exit !(NR == 1 && exact == 1) }'
done

sudo -u alloy head -n 1 /var/log/mapleland-api/mapleland-api.json | jq -e \
  'has("@timestamp") and has("log.level") and .["service.name"] == "mapleland-api"'
```

Alloy의 authenticated scrape는 `up{job="mapleland-api"} == 1`과 component debug 상태로 다시 확인한다. 같은 bearer 검증이 container IP 요청에도 적용되므로 shared Docker network peer는 token 없이는 scrape할 수 없다. token 값 자체는 어떤 검증 출력에도 남기지 않는다.

PromQL:

```promql
up{job="integrations/unix",deployment_environment="prod",cloud_provider="oci"}
up{job="mapleland-api",service_name="mapleland-api",deployment_environment="prod"}
count({deployment_environment="prod",cloud_provider="oci"}) by (job)
process_resident_memory_bytes{job="alloy",deployment_environment="prod"}
rate(process_cpu_seconds_total{job="alloy",deployment_environment="prod"}[5m])
```

LogQL:

```logql
{service_name="mapleland-api",deployment_environment="prod",cloud_provider="oci"} | json | __error__=""
{service_name="mapleland-api",deployment_environment="prod"} | json | event_action!=""
{service_name="mapleland-api",deployment_environment="prod",level="ERROR"} | json | error_stack_trace!=""
```

JSON parse failure는 다음 두 counter의 차이로 본다. 정상 운영에서는 0이어야 한다.

```promql
loki_process_custom_ecs_log_lines_total{job="alloy"}
- loki_process_custom_ecs_valid_json_lines_total{job="alloy"}
```

### rotation, positions, app 독립성

Spring/Logback이 열고 있는 `mapleland-api.json`을 운영자가 직접 rename하면 file descriptor와 rolling state를 교란할 수 있으므로 금지한다. config에 미리 선언한 별도 exact path `observability-rotation-test.json`에서만 rotation을 검증한다. fixture에는 식별자나 secret을 넣지 않는다.

```bash
fixture=/var/log/mapleland-api/observability-rotation-test.json
rotation_id="observability-rotation-$(date -u +%s)"
before_message="${rotation_id}-before"
after_message="${rotation_id}-after"

make_rotation_event() {
  jq -cn \
    --arg timestamp "$(date --iso-8601=ns)" \
    --arg message "$1" \
    '{
      "@timestamp": $timestamp,
      "log.level": "INFO",
      "log.logger": "observability.rotation",
      "process.pid": 0,
      "process.thread.name": "runbook",
      "service.name": "mapleland-api",
      "service.environment": "prod",
      "service.version": "runbook",
      "message": $message,
      "event.action": "observability.rotation-test",
      "event.outcome": "success"
    }'
}

make_rotation_event "$before_message" | sudo tee "$fixture" >/dev/null
sudo chown root:alloy "$fixture"
sudo chmod 0640 "$fixture"

# Wait until before_message is visible in Loki, then rotate only the fixture.
sudo mv "$fixture" "${fixture}.${rotation_id}.rotated"
make_rotation_event "$after_message" | sudo tee "$fixture" >/dev/null
sudo chown root:alloy "$fixture"
sudo chmod 0640 "$fixture"
```

Loki에서 두 message를 각각 exact match로 조회하고 각 count가 정확히 1인지 확인한다. `event_action="observability.rotation-test"`도 JSON field로 조회돼야 한다. 두 결과가 확인된 뒤에만 fixture와 `.rotated` 파일을 제거한다. rolled fixture는 source glob에 포함되지 않으므로 새 path로 0부터 다시 읽히지 않는다.

정상 file position resume은 active file이 10 MiB rollover 경계에서 충분히 떨어진 것을 확인한 뒤 Alloy를 60초 미만 정지하고 검증한다.

1. API health/smoke는 계속 성공한다.
2. local JSON file 크기와 line 수가 증가한다.
3. Alloy 재시작 후 active file의 저장 offset 다음 backlog가 Loki에 도착한다.
4. 동일 message가 중복되지 않는다.

이 검증은 Alloy 중단이 Spring Boot 기동과 요청 처리에 연결되지 않았다는 증거다. Grafana Cloud 전송 실패가 Alloy의 유한 retry를 모두 소진한 경우는 normal restart 검증과 다르며 아래 수동 replay 절차를 사용한다.

### 성능 및 수집량

변경 전 loopback 대표 API 30회 기준은 평균 2.365 ms, p95 3.494 ms이며 관찰된 startup은 17.742초였다. 같은 호스트·endpoint·횟수로 배포 후 다시 측정한다. 환경 노이즈를 고려해 p95 20% 또는 5 ms 중 큰 값 이상의 증가가 반복되면 회귀로 간주하고 원인을 조사한다.

Free Tier go/no-go는 정상 traffic을 포함해 최소 30분 관찰한 뒤 다음 내부 budget을 **모두** 만족해야 한다. 이 값은 외부 plan quota 자체가 아니라 단일-host 1차 rollout의 보수적 운영 상한이다.

- `deployment_environment="prod"` 전체 active series: 5,000 이하
- 이번 rollout의 active-series 증가분: 2,500 이하
- Loki 전송량 15분 rate의 일일 환산: 250 MiB/day 이하
- Alloy RSS: 256 MiB 이하
- Alloy CPU: 최대 0.10 core 이하
- ECS parse failure와 `loki_write_dropped_entries/bytes`: 0

```promql
count({deployment_environment="prod"})
max_over_time(process_resident_memory_bytes{job="alloy"}[30m])
max_over_time((sum(rate(process_cpu_seconds_total{job="alloy"}[5m])))[30m:])
max_over_time((sum(rate(loki_write_sent_bytes_total{job="alloy"}[5m])))[30m:]) * 86400
increase(loki_write_dropped_entries_total{job="alloy"}[30m])
increase(loki_write_dropped_bytes_total{job="alloy"}[30m])
increase(loki_process_custom_ecs_log_lines_total{job="alloy"}[30m])
- increase(loki_process_custom_ecs_valid_json_lines_total{job="alloy"}[30m])
```

250 MiB/day gate의 비교값은 `262144000` bytes다. Active-series baseline은 새 app image 재생성 직전에 기록한다. 반복 Hikari WARN을 포함한 실제 log rate로 이 gate를 넘으면 dashboard/alert 완료나 unpause로 진행하지 않는다. 원인 로그를 임의 drop하지 않고 label/metric allowlist와 수집 범위를 재검토하거나 Alloy를 stop해 local rotation에만 보존한 뒤 재승인을 받는다. Scrape/allowlist 또는 budget 변경은 근거와 함께 이 문서와 dashboard source를 같이 수정한다.

## Dashboard와 alert

- folder UID/title: `mapleland` / `Mapleland`
- dashboard UID/title: `mapleland-production-overview` / `Mapleland / Production Overview`
- datasource: `grafanacloud-prom`, `grafanacloud-logs`
- source: `deploy/observability/grafana/`

Alert 정책:

- application scrape down: `up < 1`, 5분; No Data=`Alerting`, execution error=`Error`
- traffic-aware 5xx: 10분 5xx 비율 5% 초과이면서 같은 기간 20 request 이상, 10분 지속; No Data=`OK`
- writable disk available 10% 미만, 15분 지속; No Data=`OK`

기존 notification policy와 기본 email contact point는 수정하지 않는다. Rules는 데이터 도착 전에는 paused로 생성하고 각 query가 정상 데이터로 평가되는 것을 확인한 후에만 unpause한다. 임계값을 강제로 위반시키거나 test notification을 발송하지 않는다.

## 롤백과 복구

### 애플리케이션 코드/ECS

위에서 생성한 `/root/mapleland-observability-rollback.env`의 exact local image tag와 Compose/`.env`/deployment-script backup을 사용해 app service만 `--pull never --force-recreate`로 재생성한다. Rollback 뒤 실제 container image ID, management/public health와 대표 API를 다시 확인한다. 이전 버전에는 Loki4j가 있으므로 이전 `GRAFANA_CLOUD_*` app 환경도 root-only backup에서 함께 복원한다. ECS만 되돌릴 때도 이전 image/config를 사용하며 운영 파일을 임의로 in-place 변환하지 않는다.

### Alloy

pipeline만 즉시 중지할 때는 package와 positions를 보존한다.

```bash
sudo systemctl disable --now alloy
```

이번이 최초 설치라 되돌아갈 Alloy package가 없으면 `purge`하지 않고 package만 제거한다. `/etc/alloy`, root-only env와 `/var/lib/alloy`의 positions/WAL은 incident 확인과 재적용을 위해 보존하고, token은 Grafana Cloud에서 별도로 revoke한다.

```bash
sudo systemctl mask --now alloy.service
sudo apt-get remove -y alloy
```

이전 package로 실제 downgrade할 때는 위 v1.16.1 rollback 절차를 그대로 적용한다. 즉 checksum을 통과한 exact deb와 호환 config를 준비하고, extracted 이전 binary로 먼저 validate한 뒤 unit을 **unmasked-stopped** 상태로 둔 채 `--allow-downgrades`를 실행한다. Package 작업 뒤 권한/ACL을 다시 강제하고 설치된 이전 binary를 validate한 다음에만 explicit start/ready 검증을 수행한다. Upgrade/downgrade package 설치 중 unit을 mask하는 절차는 사용하지 않는다.

설정만 되돌릴 때도 `/etc/alloy/config.alloy` 백업을 복원하고 같은 root EnvironmentFile validation 후 restart한다. app는 local file에 계속 기록하므로 Alloy 장애 중에도 서비스는 동작한다. primary positions directory는 임의로 삭제하거나 초기화하지 않는다.

### 전송 누락 복구

Prometheus remote write는 disk WAL을 사용하지만 stable `loki.write`는 bounded memory queue와 유한 retry를 사용한다. `max_backoff_retries=10`을 소진하면 `loki_write_dropped_entries_total`/`dropped_bytes_total`이 증가하고, file source position은 이미 전진했으므로 Alloy restart만으로 그 line이 재전송되지 않는다. active exact-path source도 Alloy가 읽지 못한 채 rollover된 과거 파일을 자동 glob하지 않는다.

1. disk alert, `du -sh /var/log/mapleland-api`, 아래 증가량과 incident 시작/종료 시각을 기록한다. dropped counter는 수량만 알려 주고 어떤 line인지는 알려 주지 않으므로 Loki의 마지막 수신 event와 local JSON을 stream level별로 대조한다.

   ```promql
   increase(loki_write_dropped_entries_total{job="alloy"}[15m])
   increase(loki_write_dropped_bytes_total{job="alloy"}[15m])
   ```

2. credential/network/config 오류를 해결하되 primary `/var/lib/alloy/data`와 local JSON을 보존한다. 새 event가 정상 도착하는 것을 먼저 확인한다.
3. 누락이 의심되는 최소 `[from, to]` 범위를 먼저 **watched glob 밖의 quarantine**에 만든다. `/var/log/mapleland-api`는 deploy user가 읽을 수 없으므로 glob 확장부터 JSON 생성까지 root bash 안에서 수행한다. Pipeline은 `pipefail`이고, 0 byte·0 event·비-object·`@timestamp` 누락 중 하나라도 발견되면 candidate를 설치하지 않는다. 이 단계에서는 `.json` 확장자를 사용하지 않아 Alloy가 읽을 수 없다.

   ```bash
   from='2026-01-01T00:00:00Z' # replace with verified first missing timestamp
   to='2026-01-01T00:10:00Z'   # replace with verified last missing timestamp
   incident='incident-YYYYMMDDTHHMMSSZ'
   printf '%s\n' "$incident" | grep -Eq '^incident-[A-Za-z0-9._-]+$'
   candidate="/var/lib/alloy/recovery/quarantine/${incident}.candidate"

   sudo env FROM="$from" TO="$to" CANDIDATE="$candidate" \
     bash -o pipefail -seu <<'ROOT_BASH'
     shopt -s nullglob
     files=(/var/log/mapleland-api/mapleland-api*.json)
     ((${#files[@]} > 0))
     tmp="${CANDIDATE}.tmp"
     trap 'rm -f "$tmp"' EXIT

     jq -sc --arg from "$FROM" --arg to "$TO" '
       sort_by(.["@timestamp"])[] |
       select(.["@timestamp"] >= $from and .["@timestamp"] <= $to)
     ' "${files[@]}" >"$tmp"
     test -s "$tmp"
     jq -se '
       length > 0 and
       all(.[]; type == "object" and
                has("@timestamp") and
                (.["@timestamp"] | type == "string"))
     ' "$tmp" >/dev/null
     chown root:alloy "$tmp"
     chmod 0640 "$tmp"
     mv "$tmp" "$CANDIDATE"
     trap - EXIT
ROOT_BASH

   candidate_count="$(sudo jq -s 'length' "$candidate")"
   test "$candidate_count" -gt 0
   ```

4. Replay 전에 dedup gate를 통과한다. Candidate와 같은 시간 범위의 Loki raw line을 canonical JSON(`jq -S -c`)으로 정규화해 SHA-256을 비교하고, `@timestamp`, `log.logger`, `message`, `error.type` tuple은 운영자가 결과를 찾는 보조 key로 사용한다. 이미 수신된 fingerprint는 candidate에서 제거하고 다시 JSON/nonempty 검증한다. 운영 기록에 `candidate_count`, `confirmed_missing_count`, `already_present_count`를 남기며, 아래 두 test가 성공하지 않으면 watched directory로 이동하지 않는다.

   ```bash
   confirmed_missing_count='<replace-with-reviewed-missing-count>'
   already_present_count='<replace-with-reviewed-present-count>'
   printf '%s\n' "$confirmed_missing_count" | grep -Eq '^[0-9]+$'
   printf '%s\n' "$already_present_count" | grep -Eq '^[0-9]+$'
   test "$already_present_count" -eq 0
   test "$confirmed_missing_count" -eq "$candidate_count"

   replay_tmp="/var/lib/alloy/recovery/${incident}.tmp"
   replay_file="/var/lib/alloy/recovery/${incident}.json"
   sudo test ! -e "$replay_file"
   sudo env SOURCE="$candidate" TARGET="$replay_tmp" INCIDENT="$incident" \
     bash -o pipefail -seu <<'ROOT_BASH'
     trap 'rm -f "$TARGET"' EXIT
     jq -c --arg incident "$INCIDENT" '
       .["mapleland.recovery.id"] = $incident |
       .["mapleland.recovery.mode"] = "original-timestamp"
     ' "$SOURCE" >"$TARGET"
     test -s "$TARGET"
     jq -se 'length > 0 and all(.[]; type == "object")' \
       "$TARGET" >/dev/null
     chown root:alloy "$TARGET"
     chmod 0640 "$TARGET"
     trap - EXIT
ROOT_BASH
   sudo mv "$replay_tmp" "$replay_file"
   ```

5. 원래 `@timestamp`가 Grafana Cloud의 허용 out-of-order window 안인지 먼저 확인한다. 너무 오래된 event를 현재 시각으로 바꾸면 원래 timeline을 왜곡하므로 자동 retimestamp하지 않는다. Backend가 거부하고 forensic import가 별도 승인된 경우에도 전체 original replay를 다시 쓰지 않는다. 먼저 file source가 EOF까지 읽고 `loki_write` retry/drop counter가 안정된 것을 확인한 뒤 original replay file을 quarantine으로 이동한다. Loki 결과로 **확정 rejected subset만** 새 candidate로 만들고, accepted tuple이 0인지 dedup gate를 다시 수행한다.

   ```bash
   attempted="/var/lib/alloy/recovery/quarantine/${incident}.attempted"
   sudo mv "/var/lib/alloy/recovery/${incident}.json" "$attempted"

   # Create this file from the confirmed rejected subset only; never copy the
   # whole attempted file after a partial acceptance.
   rejected="/var/lib/alloy/recovery/quarantine/${incident}.rejected.candidate"
   sudo test -s "$rejected"
   sudo jq -se 'length > 0 and all(.[]; type == "object")' \
     "$rejected" >/dev/null
   rejected_count="$(sudo jq -s 'length' "$rejected")"
   confirmed_rejected_count='<replace-with-reviewed-rejected-count>'
   accepted_in_rejected_count='<replace-with-reviewed-present-count>'
   printf '%s\n' "$confirmed_rejected_count" | grep -Eq '^[0-9]+$'
   printf '%s\n' "$accepted_in_rejected_count" | grep -Eq '^[0-9]+$'
   test "$accepted_in_rejected_count" -eq 0
   test "$confirmed_rejected_count" -eq "$rejected_count"

   retimestamped="/var/lib/alloy/recovery/${incident}-retimestamped.json"
   sudo test ! -e "$retimestamped"
   sudo env SOURCE="$rejected" TARGET="$retimestamped" INCIDENT="$incident" \
     bash -o pipefail -seu <<'ROOT_BASH'
     tmp="${TARGET}.tmp"
     trap 'rm -f "$tmp"' EXIT
     jq -c --arg incident "$INCIDENT" '
       .["mapleland.recovery.original_timestamp"] = .["@timestamp"] |
       .["mapleland.recovery.id"] = $incident |
       .["mapleland.recovery.mode"] = "forensic-retimestamp" |
       .["@timestamp"] = (now | todateiso8601)
     ' "$SOURCE" >"$tmp"
     test -s "$tmp"
     jq -se 'length > 0 and all(.[]; type == "object")' \
       "$tmp" >/dev/null
     chown root:alloy "$tmp"
     chmod 0640 "$tmp"
     mv "$tmp" "$TARGET"
     trap - EXIT
ROOT_BASH
   ```
6. recovery source는 primary source와 별도 positions를 사용하지만 동일한 네 label allowlist와 ECS parser를 거친다. `mapleland.recovery.id`별 실제 수신 count가 expected count와 같고, parse-failure delta와 `loki_write_dropped_*` 추가 증가가 0이며, 원본 tuple 중 duplicate가 0인지 확인한다.
7. 검증과 retry가 완전히 끝난 replay file만 quarantine의 `.sent` 파일로 이동한다. Top-level recovery directory에는 동시에 하나의 `.json`만 두고, incident 종료 후 보존 정책에 따라 제거한다. Primary positions는 삭제하지 않는다.

positions entry 자체가 손상된 경우 config는 loss avoidance를 위해 해당 file을 offset 0부터 다시 읽는다. 이 예외 상황에서는 누락보다 중복 가능성을 선택한 것이므로 restart 전에 local file과 Loki 경계를 기록하고 restart 뒤 exact-message duplicate를 확인한다.

### Grafana

Dashboard/alert 변경 전 version과 rule JSON을 저장한다. 롤백은 이전 dashboard version 복원 또는 이 PR source의 직전 Git version을 overwrite하고, 새 rule은 먼저 pause한다. 기존 dashboard, datasource, contact point, notification route, MCP connection은 삭제하지 않는다.

## 위험 리뷰와 후속 작업

- Hikari connection validation warning은 최근 로그의 주요 반복 신호다. 이번 PR은 원인을 수정하지 않는다. DB `maxLifetime`/server timeout 정합성과 발생량을 별도 Issue로 추적한다.
- 인증 재발급 경로의 NullPointerException은 이번 PR에서 수정하지 않는다. 재현 조건과 null contract를 별도 Issue로 추적한다.
- 이번 적용에서 `/opt/mapleland/.env`는 `root:root 0600`으로 낮추지만, 기존 저장소의 애플리케이션 인증/Firebase secret 자체의 저장·rotation 방식은 별도 보안 hardening이 필요하다. 실제 값은 Issue/PR/log에 복사하지 않는다.
- `NoticeApiTest`가 외부 사이트를 timeout 없이 호출해 CI가 비결정적일 수 있다. hermetic fixture 전환을 후속 처리한다.
- 첫 단계는 단일 host의 `instance`만 추가한다. host 수가 늘면 hostname naming contract와 series budget을 먼저 갱신한다.
