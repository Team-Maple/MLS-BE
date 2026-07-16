#!/usr/bin/env bash
set -euo pipefail

TEST_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
readonly TEST_DIR
OBSERVABILITY_DIR=$(cd "${TEST_DIR}/.." && pwd)
readonly OBSERVABILITY_DIR
readonly UPDATE_SCRIPT=${OBSERVABILITY_DIR}/update-api.sh
readonly OVERRIDE_SOURCE=${OBSERVABILITY_DIR}/docker-compose.override.example.yml
readonly IMAGE_REF=ghcr.io/team-maple/mls-be/mapleland-api@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

fail() {
  echo "update-api test failed: $*" >&2
  exit 1
}

assert_not_contains() {
  local haystack=$1
  local needle=$2
  [[ ${haystack} != *"${needle}"* ]] \
    || fail "output unexpectedly contains: ${needle}"
}

WORK_DIR=$(mktemp -d)
readonly WORK_DIR
trap 'rm -rf "${WORK_DIR}"' EXIT
readonly FAKE_BIN=${WORK_DIR}/bin
mkdir -p "${FAKE_BIN}"

# The single-quoted bodies are written to executable mock scripts.
# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'case ${1:-} in' \
  '  -c) if [[ ${2:-} == %u ]]; then printf "%s\\n" 0; else case ${3##*/} in docker-compose.yml|docker-compose.observability.yml) printf "%s\\n" 640 ;; *) printf "%s\\n" 600 ;; esac; fi ;;' \
  '  -u) printf "%s\\n" 0 ;;' \
  '  *) exit 1 ;;' \
  'esac' > "${FAKE_BIN}/stat"

printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${FAKE_BIN}/chown"
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${FAKE_BIN}/curl"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'state_file=${FAKE_DOCKER_STATE:?}' \
  'up_log=${FAKE_DOCKER_UP_LOG:?}' \
  'case ${1:-} in' \
  '  login) cat >/dev/null; exit 0 ;;' \
  '  logout|pull) exit 0 ;;' \
  '  image)' \
  '    case ${2:-} in' \
  '      inspect)' \
  '        if [[ ${3:-} == --format || ${4:-} == --format ]]; then' \
  '          printf "%s\\n" sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb' \
  '        fi' \
  '        exit 0 ;;' \
  '      tag) exit 0 ;;' \
  '    esac ;;' \
  '  compose)' \
  '    shift' \
  '    combined=false' \
  '    while [[ ${1:-} == -f || ${1:-} == --env-file ]]; do' \
  '      if [[ ${1:-} == -f && ${2:-} == *docker-compose.observability.yml ]]; then combined=true; fi' \
  '      shift 2' \
  '    done' \
  '    case ${1:-} in' \
  '      config)' \
  '        [[ ${2:-} == --images ]] && printf "%s\\n" ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64' \
  '        exit 0 ;;' \
  '      ps)' \
  '        all=false' \
  '        shift' \
  '        for argument in "$@"; do [[ $argument == --all ]] && all=true; done' \
  '        if [[ $(cat "$state_file") == old ]]; then printf "%s\\n" old-container; elif [[ ${FAKE_NEW_STATE:-running} != exited || $all == true ]]; then printf "%s\\n" new-container; fi' \
  '        exit 0 ;;' \
  '      up)' \
  '        if [[ $combined == true ]]; then printf "%s\\n" combined >> "$up_log"; printf "%s\\n" new > "$state_file"; else printf "%s\\n" base >> "$up_log"; printf "%s\\n" old > "$state_file"; fi' \
  '        exit 0 ;;' \
  '    esac ;;' \
  '  inspect)' \
  '    target=${2:-}' \
  '    format=${4:-}' \
  '    case $format in' \
  '      *RestartCount*) if [[ $(cat "$state_file") == new ]]; then printf "%s\\n" "${FAKE_NEW_RESTART_COUNT:-0}"; else printf "%s\\n" 0; fi ;;' \
  '      *json*.State*) printf "%s\\n" "{\\"Status\\":\\"running\\",\\"ExitCode\\":0,\\"OOMKilled\\":false}" ;;' \
  '      *State.Status*) if [[ $(cat "$state_file") == new ]]; then printf "%s\\n" "${FAKE_NEW_STATE:-running}"; else printf "%s\\n" running; fi ;;' \
  '      *State.Health*) printf "%s\\n" healthy ;;' \
  '      *Config.Env*)' \
  '        if [[ $target == new-container && ${FAKE_NEW_HAS_LEGACY:-0} == 1 ]]; then printf "%s\\n" GRAFANA_CLOUD_PASSWORD=fixture-legacy-secret; else printf "%s\\n" SPRING_PROFILES_ACTIVE=prod; fi ;;' \
  '      *.Image*) [[ $target == old-container ]] && printf "%s\\n" sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa || printf "%s\\n" sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb ;;' \
  '    esac' \
  '    exit 0 ;;' \
  '  logs) printf "%s\\n" "fixture startup failure"; exit 0 ;;' \
  'esac' \
  'exit 1' > "${FAKE_BIN}/docker"

chmod +x "${FAKE_BIN}"/*

setup_fixture() {
  local root=$1
  mkdir -p "${root}/opt/mapleland" "${root}/run"
  cp "${OVERRIDE_SOURCE}" "${root}/opt/mapleland/docker-compose.observability.yml"
  # Literal Compose interpolation expressions are intentional fixture data.
  # shellcheck disable=SC2016
  printf '%s\n' \
    'services:' \
    '  mapleland-api:' \
    '    image: ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64' \
    '    environment:' \
    '      - GRAFANA_CLOUD_URL=${GRAFANA_CLOUD_URL}' \
    '      - GRAFANA_CLOUD_USERNAME=${GRAFANA_CLOUD_USERNAME}' \
    '      - GRAFANA_CLOUD_PASSWORD=${GRAFANA_CLOUD_PASSWORD}' \
    '      - SPRING_PROFILES_ACTIVE=prod' \
    > "${root}/opt/mapleland/docker-compose.yml"
  printf '%s\n' \
    'GRAFANA_CLOUD_URL=https://example.invalid' \
    'GRAFANA_CLOUD_USERNAME=fixture-user' \
    'GRAFANA_CLOUD_PASSWORD=fixture-legacy-secret' \
    'MANAGEMENT_SCRAPE_TOKEN=fixture-management-secret-0123456789' \
    'SERVICE_VERSION=f4bf228b934959be125a72540c91e43f003b7b6e' \
    > "${root}/opt/mapleland/.env"
  printf '%s\n' \
    'GHCR_USER=fixture-user' \
    'GHCR_TOKEN=fixture-ghcr-secret' \
    > "${root}/opt/mapleland/ghcr.env"
  printf '%s\n' old > "${root}/state"
  : > "${root}/up.log"
}

run_update() {
  local root=$1
  shift
  env \
    PATH="${FAKE_BIN}:${PATH}" \
    MAPLELAND_UPDATE_TEST_MODE=1 \
    MAPLELAND_UPDATE_TEST_ROOT="${root}" \
    FAKE_DOCKER_STATE="${root}/state" \
    FAKE_DOCKER_UP_LOG="${root}/up.log" \
    "$@" \
    "${BASH}" "${UPDATE_SCRIPT}" "${IMAGE_REF}"
}

success_root=${WORK_DIR}/success
setup_fixture "${success_root}"
success_output=$(run_update "${success_root}" 2>&1) \
  || fail "healthy immutable deployment should succeed: ${success_output}"
[[ $(cat "${success_root}/state") == new ]] || fail 'new container was not selected'
[[ $(cat "${success_root}/up.log") == combined ]] \
  || fail 'deployment did not use the reviewed override'
if grep -Eq 'GRAFANA_CLOUD_(URL|USERNAME|PASSWORD)' \
  "${success_root}/opt/mapleland/docker-compose.yml" \
  "${success_root}/opt/mapleland/.env"; then
  fail 'legacy appender environment was not removed after readiness'
fi
assert_not_contains "${success_output}" 'fixture-ghcr-secret'
assert_not_contains "${success_output}" 'fixture-legacy-secret'
assert_not_contains "${success_output}" 'fixture-management-secret'

rollback_root=${WORK_DIR}/rollback
setup_fixture "${rollback_root}"
if rollback_output=$(run_update "${rollback_root}" FAKE_NEW_HAS_LEGACY=1 2>&1); then
  fail 'candidate containing a legacy Grafana credential should fail'
fi
[[ $(cat "${rollback_root}/state") == old ]] || fail 'previous container was not restored'
[[ $(paste -sd, "${rollback_root}/up.log") == combined,base ]] \
  || fail 'first-rollout rollback did not use the preserved base contract'
grep -Eq 'GRAFANA_CLOUD_PASSWORD' "${rollback_root}/opt/mapleland/.env" \
  || fail 'failed deployment unexpectedly removed rollback credentials'
assert_not_contains "${rollback_output}" 'fixture-ghcr-secret'
assert_not_contains "${rollback_output}" 'fixture-legacy-secret'
assert_not_contains "${rollback_output}" 'fixture-management-secret'
[[ -f ${rollback_root}/var/log/mapleland-deploy/last-failure/state.json ]] \
  || fail 'failed candidate state was not preserved before rollback'
[[ $(cat "${rollback_root}/var/log/mapleland-deploy/last-failure/container.log") == \
  'fixture startup failure' ]] \
  || fail 'failed candidate logs were not preserved before rollback'

restart_root=${WORK_DIR}/restart
setup_fixture "${restart_root}"
if restart_output=$(run_update "${restart_root}" FAKE_NEW_RESTART_COUNT=1 2>&1); then
  fail 'a candidate restart should fail the deployment'
fi
[[ $(cat "${restart_root}/state") == old ]] \
  || fail 'restarted candidate did not roll back to the previous image'
[[ ${restart_output} == *'candidate entered a restart loop (restart_count=1)'* ]] \
  || fail 'candidate restart was not diagnosed explicitly'
[[ -f ${restart_root}/var/log/mapleland-deploy/last-failure/container.log ]] \
  || fail 'restart-loop diagnostics were not preserved before rollback'
assert_not_contains "${restart_output}" 'fixture-management-secret'

exited_root=${WORK_DIR}/exited
setup_fixture "${exited_root}"
if exited_output=$(run_update "${exited_root}" FAKE_NEW_STATE=exited 2>&1); then
  fail 'an exited candidate should fail the deployment'
fi
[[ $(cat "${exited_root}/state") == old ]] \
  || fail 'exited candidate did not roll back to the previous image'
[[ ${exited_output} == *'candidate stopped before readiness (state=exited)'* ]] \
  || fail 'exited candidate was not diagnosed explicitly'
[[ -f ${exited_root}/var/log/mapleland-deploy/last-failure/container.log ]] \
  || fail 'exited candidate diagnostics were not preserved before rollback'
assert_not_contains "${exited_output}" 'fixture-management-secret'

gateway_sudo=${WORK_DIR}/gateway-sudo
gateway_log=${WORK_DIR}/gateway.log
# The single-quoted body is written to the executable mock.
# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'printf "%s\\n" "$*" >> "${FAKE_GATEWAY_LOG:?}"' \
  > "${gateway_sudo}"
chmod +x "${gateway_sudo}"

preflight_sha=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
update_sha=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
override_sha=cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc
: > "${gateway_log}"
env SSH_ORIGINAL_COMMAND="preflight ${preflight_sha} ${update_sha} ${override_sha}" \
    MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
    MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
    FAKE_GATEWAY_LOG="${gateway_log}" \
    "${BASH}" "${UPDATE_SCRIPT}"
[[ $(cat "${gateway_log}") == \
  "-n /opt/mapleland/preflight-host.sh ${preflight_sha} ${update_sha} ${override_sha}" ]] \
  || fail 'gateway did not dispatch the exact preflight allowlist'

: > "${gateway_log}"
env SSH_ORIGINAL_COMMAND="deploy ${preflight_sha} ${update_sha} ${override_sha} ${IMAGE_REF}" \
    MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
    MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
    FAKE_GATEWAY_LOG="${gateway_log}" \
    "${BASH}" "${UPDATE_SCRIPT}"
expected_gateway_log=$(printf '%s\n%s' \
  "-n /opt/mapleland/preflight-host.sh ${preflight_sha} ${update_sha} ${override_sha}" \
  "-n /opt/mapleland/update-api.sh ${IMAGE_REF}")
[[ $(cat "${gateway_log}") == "${expected_gateway_log}" ]] \
  || fail 'gateway did not keep preflight and immutable deploy in the allowlist'

: > "${gateway_log}"
if env SSH_ORIGINAL_COMMAND="preflight ${preflight_sha} ${update_sha} ${override_sha}"$'\nuname -a' \
    MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
    MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
    FAKE_GATEWAY_LOG="${gateway_log}" \
    "${BASH}" "${UPDATE_SCRIPT}" >/dev/null 2>&1; then
  fail 'gateway should reject multiline input'
fi
[[ ! -s ${gateway_log} ]] || fail 'rejected gateway input reached sudo'

: > "${gateway_log}"
if printf 'preflight %s %s %s\n' \
    "${preflight_sha}" "${update_sha}" "${override_sha}" |
  env SSH_ORIGINAL_COMMAND='uname -a' \
    MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
    MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
    FAKE_GATEWAY_LOG="${gateway_log}" \
    "${BASH}" "${UPDATE_SCRIPT}" >/dev/null 2>&1; then
  fail 'gateway should not accept an allowlisted command from stdin'
fi
[[ ! -s ${gateway_log} ]] || fail 'stdin bypass reached sudo'

echo 'update-api tests passed'
