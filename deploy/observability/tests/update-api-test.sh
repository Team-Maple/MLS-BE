#!/usr/bin/env bash
set -euo pipefail

TEST_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
readonly TEST_DIR
OBSERVABILITY_DIR=$(cd "${TEST_DIR}/.." && pwd)
readonly OBSERVABILITY_DIR
REPOSITORY_DIR=$(cd "${OBSERVABILITY_DIR}/../.." && pwd)
readonly REPOSITORY_DIR
readonly UPDATE_SCRIPT=${OBSERVABILITY_DIR}/update-api.sh
readonly OVERRIDE_SOURCE=${OBSERVABILITY_DIR}/docker-compose.override.example.yml
readonly DEPLOY_WORKFLOW=${REPOSITORY_DIR}/.github/workflows/deploy-oci.yml
readonly IMAGE_REF=ghcr.io/team-maple/mls-be/mapleland-api@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
REAL_DOCKER=$(command -v docker || true)
readonly REAL_DOCKER
REAL_MKTEMP=$(command -v mktemp)
readonly REAL_MKTEMP
REAL_CHMOD=$(command -v chmod)
readonly REAL_CHMOD

fail() {
  echo "update-api test failed: $*" >&2
  exit 1
}

assert_contains() {
  local haystack=$1
  local needle=$2
  [[ ${haystack} == *"${needle}"* ]] || fail "output does not contain: ${needle}"
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
  'while [[ ${1:-} == --* ]]; do shift; done' \
  'shift' \
  'exec "$@"' \
  > "${FAKE_BIN}/timeout"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'exit "${FAKE_FLOCK_EXIT:-0}"' \
  > "${FAKE_BIN}/flock"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'if [[ ${FAKE_DIAGNOSTIC_MKTEMP_FAIL:-0} == 1 && $* == *mapleland-deploy/.last-*-failure.* ]]; then' \
  '  exit 1' \
  'fi' \
  'exec "${REAL_MKTEMP_BIN:?}" "$@"' \
  > "${FAKE_BIN}/mktemp"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'mode=${1:-}' \
  'shift' \
  'for path in "$@"; do' \
  '  if [[ ${path} != "${FAKE_CHMOD_ALLOWED_ROOT:?}"/* ]]; then' \
  '    printf "unsafe chmod target: %s %s\n" "${mode}" "${path}" >&2' \
  '    exit 99' \
  '  fi' \
  'done' \
  'exec "${REAL_CHMOD_BIN:?}" "${mode}" "$@"' \
  > "${FAKE_BIN}/chmod"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'state_file=${FAKE_DOCKER_STATE:?}' \
  'url=' \
  'write_status=false' \
  'use_stdin_config=false' \
  'for argument in "$@"; do' \
  '  [[ ${argument} == http://* ]] && url=${argument}' \
  '  [[ ${argument} == %{http_code} ]] && write_status=true' \
  '  [[ ${argument} == --config ]] && use_stdin_config=true' \
  'done' \
  '[[ ${use_stdin_config} == true ]] && cat >/dev/null' \
  'should_fail=false' \
  'if [[ $(cat "${state_file}") == old && ${FAKE_OLD_SMOKE_FAIL:-0} == 1 ]]; then' \
  '  should_fail=true' \
  'elif [[ $(cat "${state_file}") == new ]]; then' \
  '  if [[ ${url} == *:18080/* && ${FAKE_MANAGEMENT_SMOKE_FAIL:-0} == 1 ]]; then' \
  '    should_fail=true' \
  '  elif [[ ${url} == *:18080/actuator/prometheus && ${FAKE_PROMETHEUS_SMOKE_FAIL:-0} == 1 ]]; then' \
  '    should_fail=true' \
  '  elif [[ ${url} == *:8080/* && ${FAKE_PUBLIC_SMOKE_FAIL:-0} == 1 ]]; then' \
  '    should_fail=true' \
  '  fi' \
  'fi' \
  'if [[ $(cat "${state_file}") == new && ${url} == *:8080/* && ${FAKE_PUBLIC_SMOKE_REDIRECT:-0} == 1 ]]; then' \
  '  [[ ${write_status} == true ]] && printf "%s" 302' \
  '  exit 0' \
  'fi' \
  'if [[ ${should_fail} == true ]]; then' \
  '  [[ ${write_status} == true ]] && printf "%s" 503' \
  '  exit 22' \
  'fi' \
  '[[ ${write_status} == true ]] && printf "%s" 200' \
  'exit 0' \
  > "${FAKE_BIN}/curl"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'state_file=${FAKE_DOCKER_STATE:?}' \
  'up_log=${FAKE_DOCKER_UP_LOG:?}' \
  'action_log=${FAKE_DOCKER_ACTION_LOG:?}' \
  'version_log=${FAKE_DOCKER_VERSION_LOG:?}' \
  'rollback_marker=${state_file}.rollback' \
  'printf "%s\n" "$*" >> "${action_log}"' \
  'case ${1:-} in' \
  '  login)' \
  '    cat >/dev/null' \
  '    mkdir -p "${DOCKER_CONFIG:?}"' \
  '    printf "%s\n" "{}" > "${DOCKER_CONFIG}/config.json"' \
  '    exit 0 ;;' \
  '  pull) exit 0 ;;' \
  '  image)' \
  '    case ${2:-} in' \
  '      inspect)' \
  '        if [[ $* == *org.opencontainers.image.revision* ]]; then' \
  '          printf "%s\n" dddddddddddddddddddddddddddddddddddddddd' \
  '        elif [[ $* == *--format* ]]; then' \
  '          printf "%s\n" sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb' \
  '        fi' \
  '        exit 0 ;;' \
  '      tag)' \
  '        [[ ${3:-} == *:rollback-* ]] && : > "${rollback_marker}"' \
  '        exit 0 ;;' \
  '    esac ;;' \
  '  compose)' \
  '    shift' \
  '    base=false' \
  '    override=false' \
  '    env_file=false' \
  '    while [[ ${1:-} == -f || ${1:-} == --env-file ]]; do' \
  '      case ${1}:${2:-} in' \
  '        -f:*docker-compose.yml) base=true ;;' \
  '        -f:*docker-compose.observability.yml) override=true ;;' \
  '        --env-file:*.env) env_file=true ;;' \
  '      esac' \
  '      shift 2' \
  '    done' \
  '    [[ ${base} == true && ${override} == true && ${env_file} == true ]] || exit 64' \
  '    case ${1:-} in' \
  '      config)' \
  '        [[ ${2:-} == --images ]] || exit 64' \
  '        if [[ ${3:-} == mapleland-api ]]; then' \
  '          printf "%s\n" ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64' \
  '        else' \
  '          printf "%s\n" ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64 ghcr.io/team-maple/mls-be/mapleland-api:worker' \
  '        fi' \
  '        exit 0 ;;' \
  '      ps)' \
  '        if [[ $(cat "${state_file}") == old ]]; then' \
  '          printf "%s\n" old-container' \
  '        else' \
  '          printf "%s\n" new-container' \
  '        fi' \
  '        exit 0 ;;' \
  '      up)' \
  '        [[ " $* " == *" --pull never "* ]] || exit 64' \
  '        [[ " $* " == *" --force-recreate "* ]] || exit 64' \
  '        [[ " $* " == *" --no-deps "* ]] || exit 64' \
  '        printf "%s\n" combined >> "${up_log}"' \
  '        printf "%s\n" "${SERVICE_VERSION:-missing}" >> "${version_log}"' \
  '        if [[ -e ${rollback_marker} ]]; then' \
  '          rm -f "${rollback_marker}"' \
  '          [[ ${FAKE_ROLLBACK_UP_FAIL:-0} != 1 ]] || exit 1' \
  '          printf "%s\n" old > "${state_file}"' \
  '        else' \
  '          printf "%s\n" new > "${state_file}"' \
  '          if [[ ${FAKE_SIGNAL_PARENT:-} == HUP || ${FAKE_SIGNAL_PARENT:-} == PIPE ]]; then' \
  '            kill -s "${FAKE_SIGNAL_PARENT}" "$PPID"' \
  '          fi' \
  '        fi' \
  '        exit 0 ;;' \
  '    esac ;;' \
  '  inspect)' \
  '    target=${2:-}' \
  '    format=${4:-}' \
  '    case ${format} in' \
  '      *RestartCount*)' \
  '        if [[ ${target} == new-container && $(cat "${state_file}") == new ]]; then' \
  '          printf "%s\n" "${FAKE_NEW_RESTART_COUNT:-0}"' \
  '        else printf "%s\n" "${FAKE_OLD_RESTART_COUNT:-0}"; fi ;;' \
  '      *json*.State*) printf "%s\n" "{\"Status\":\"running\",\"ExitCode\":0,\"OOMKilled\":false}" ;;' \
  '      *State.Status*)' \
  '        if [[ ${target} == new-container && $(cat "${state_file}") == new ]]; then' \
  '          printf "%s\n" "${FAKE_NEW_STATE:-running}"' \
  '        else printf "%s\n" running; fi ;;' \
  '      *State.Health*) printf "%s\n" healthy ;;' \
  '      *Config.Env*)' \
  '        if [[ ${target} == new-container ]]; then' \
  '          if [[ ${FAKE_CANDIDATE_SERVICE_VERSION_MISMATCH:-0} == 1 ]]; then' \
  '            printf "%s\n" SERVICE_VERSION=eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee' \
  '          else' \
  '            printf "%s\n" SERVICE_VERSION=dddddddddddddddddddddddddddddddddddddddd' \
  '          fi' \
  '        elif [[ ${FAKE_OLD_SERVICE_VERSION_INVALID:-0} == 1 ]]; then' \
  '          printf "%s\n" SERVICE_VERSION=unknown' \
  '        else' \
  '          printf "%s\n" SERVICE_VERSION=f4bf228b934959be125a72540c91e43f003b7b6e' \
  '        fi ;;' \
  '      *.Image*)' \
  '        if [[ ${target} == old-container ]]; then' \
  '          printf "%s\n" sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' \
  '        else' \
  '          printf "%s\n" sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb' \
  '        fi ;;' \
  '    esac' \
  '    exit 0 ;;' \
  '  logs) printf "%s\n" "fixture startup failure"; exit 0 ;;' \
  'esac' \
  'exit 1' \
  > "${FAKE_BIN}/docker"

chmod +x "${FAKE_BIN}"/*

setup_fixture() {
  local root=$1
  mkdir -p "${root}/opt/mapleland" "${root}/run/lock" "${root}/var/log"
  cp "${OVERRIDE_SOURCE}" "${root}/opt/mapleland/docker-compose.observability.yml"
  printf '%s\n' \
    'services:' \
    '  mapleland-api:' \
    '    image: ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64' \
    '    environment:' \
    '      SPRING_PROFILES_ACTIVE: prod' \
    '  decoy:' \
    '    image: ghcr.io/team-maple/mls-be/mapleland-api:worker' \
    > "${root}/opt/mapleland/docker-compose.yml"
  printf '%s\n' \
    'MANAGEMENT_SCRAPE_TOKEN=fixture-management-secret-0123456789' \
    'SERVICE_VERSION=f4bf228b934959be125a72540c91e43f003b7b6e' \
    > "${root}/opt/mapleland/.env"
  printf '%s\n' \
    'GHCR_USER=fixture-user' \
    'GHCR_TOKEN=fixture-ghcr-secret' \
    > "${root}/opt/mapleland/ghcr.env"
  chmod 0640 \
    "${root}/opt/mapleland/docker-compose.yml" \
    "${root}/opt/mapleland/docker-compose.observability.yml"
  chmod 0600 \
    "${root}/opt/mapleland/.env" \
    "${root}/opt/mapleland/ghcr.env"
  printf '%s\n' old > "${root}/state"
  : > "${root}/up.log"
  : > "${root}/actions.log"
  : > "${root}/versions.log"
  cp "${root}/opt/mapleland/docker-compose.yml" "${root}/compose.before"
  cp "${root}/opt/mapleland/docker-compose.observability.yml" "${root}/override.before"
  cp "${root}/opt/mapleland/.env" "${root}/env.before"
}

assert_contract_unchanged() {
  local root=$1
  cmp "${root}/compose.before" "${root}/opt/mapleland/docker-compose.yml" >/dev/null \
    || fail 'base Compose was modified'
  cmp "${root}/override.before" "${root}/opt/mapleland/docker-compose.observability.yml" >/dev/null \
    || fail 'Compose override was modified'
  cmp "${root}/env.before" "${root}/opt/mapleland/.env" >/dev/null \
    || fail 'application environment was modified'
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
    FAKE_DOCKER_ACTION_LOG="${root}/actions.log" \
    FAKE_DOCKER_VERSION_LOG="${root}/versions.log" \
    FAKE_CHMOD_ALLOWED_ROOT="${root}" \
    REAL_CHMOD_BIN="${REAL_CHMOD}" \
    REAL_MKTEMP_BIN="${REAL_MKTEMP}" \
    "$@" \
    "${BASH}" "${UPDATE_SCRIPT}" "${IMAGE_REF}"
}

defaults_root=${WORK_DIR}/defaults
setup_fixture "${defaults_root}"
if [[ -n ${REAL_DOCKER} ]]; then
  rendered_compose=$("${REAL_DOCKER}" compose \
    --env-file "${defaults_root}/opt/mapleland/.env" \
    -f "${defaults_root}/opt/mapleland/docker-compose.yml" \
    -f "${defaults_root}/opt/mapleland/docker-compose.observability.yml" \
    config --format json)
  jq -e '
    .services["mapleland-api"].environment.RECOMMENDATION_V1_ENGINE == "AURA" and
    .services["mapleland-api"].environment.RECOMMENDATION_V2_ENABLED == "false" and
    .services["mapleland-api"].environment.RECOMMENDATION_QUERY_TIMEOUT_SECONDS == "10"
  ' <<< "${rendered_compose}" >/dev/null \
    || fail 'Compose did not apply safe recommendation defaults'
fi

success_root=${WORK_DIR}/success
setup_fixture "${success_root}"
success_output=$(run_update "${success_root}" 2>&1) \
  || fail "healthy immutable deployment should succeed: ${success_output}"
[[ $(cat "${success_root}/state") == new ]] || fail 'new container was not selected'
[[ $(cat "${success_root}/up.log") == combined ]] \
  || fail 'deployment did not use the base and observability Compose files'
[[ $(cat "${success_root}/versions.log") == dddddddddddddddddddddddddddddddddddddddd ]] \
  || fail 'candidate did not receive the revision embedded in its image'
grep -Fq "pull ${IMAGE_REF}" "${success_root}/actions.log" \
  || fail 'immutable image was not pulled'
grep -Fq -- '--pull never' "${success_root}/actions.log" \
  || fail 'candidate recreation could resolve a mutable registry tag'
assert_contains "${success_output}" 'deployment phase=pull outcome=success'
assert_contains "${success_output}" 'deployment phase=recreate outcome=success'
assert_contains "${success_output}" 'deployment phase=readiness outcome=success'
assert_contains "${success_output}" 'deployment outcome=success'
assert_contains "${success_output}" 'telemetry_verification=external'
assert_contract_unchanged "${success_root}"
assert_not_contains "${success_output}" 'fixture-ghcr-secret'
assert_not_contains "${success_output}" 'fixture-management-secret'
if find "${success_root}/run" -name 'mapleland-docker-config.*' -print -quit | grep -q .; then
  fail 'temporary registry credentials were not removed'
fi

rollback_root=${WORK_DIR}/rollback
setup_fixture "${rollback_root}"
if rollback_output=$(run_update "${rollback_root}" FAKE_PUBLIC_SMOKE_FAIL=1 2>&1); then
  fail 'candidate failing the public smoke should fail the deployment'
fi
[[ $(cat "${rollback_root}/state") == old ]] || fail 'previous image was not restored'
[[ $(paste -sd, "${rollback_root}/up.log") == combined,combined ]] \
  || fail 'deployment and rollback did not use the same Compose contract'
[[ $(paste -sd, "${rollback_root}/versions.log") == \
  dddddddddddddddddddddddddddddddddddddddd,f4bf228b934959be125a72540c91e43f003b7b6e ]] \
  || fail 'candidate and rollback did not receive their own service versions'
grep -Eq '^image tag sha256:a{64} ghcr[.]io/team-maple/mls-be/mapleland-api:rollback-' \
  "${rollback_root}/actions.log" \
  || fail 'rollback tag did not preserve the exact previous image ID'
grep -Eq '^image tag ghcr[.]io/team-maple/mls-be/mapleland-api:rollback-.* ghcr[.]io/team-maple/mls-be/mapleland-api:latest-arm64$' \
  "${rollback_root}/actions.log" \
  || fail 'rollback did not restore the preserved image to the Compose tag'
assert_contains "${rollback_output}" 'deployment phase=rollback outcome=success'
assert_contains "${rollback_output}" 'deployment outcome=rolled_back'
assert_contract_unchanged "${rollback_root}"
[[ -f ${rollback_root}/var/log/mapleland-deploy/last-candidate-failure/state.json ]] \
  || fail 'failed candidate state was not preserved before rollback'
[[ $(cat "${rollback_root}/var/log/mapleland-deploy/last-candidate-failure/container.log") == \
  'fixture startup failure' ]] \
  || fail 'failed candidate logs were not preserved before rollback'
assert_not_contains "${rollback_output}" 'fixture-ghcr-secret'

redirect_root=${WORK_DIR}/redirect
setup_fixture "${redirect_root}"
if redirect_output=$(run_update "${redirect_root}" \
    FAKE_PUBLIC_SMOKE_REDIRECT=1 2>&1); then
  fail 'candidate returning a redirect from the public smoke should fail deployment'
fi
[[ $(cat "${redirect_root}/state") == old ]] \
  || fail 'public smoke redirect did not restore the previous image'
assert_contains "${redirect_output}" 'public_status=302'
assert_contains "${redirect_output}" 'deployment outcome=rolled_back'

management_root=${WORK_DIR}/management
setup_fixture "${management_root}"
if management_output=$(run_update "${management_root}" \
    FAKE_MANAGEMENT_SMOKE_FAIL=1 2>&1); then
  fail 'candidate without the management endpoint should fail the deployment'
fi
[[ $(cat "${management_root}/state") == old ]] \
  || fail 'management smoke failure did not restore the previous image'
assert_contains "${management_output}" 'management_status=503'
assert_contains "${management_output}" 'deployment outcome=rolled_back'

prometheus_root=${WORK_DIR}/prometheus
setup_fixture "${prometheus_root}"
if prometheus_output=$(run_update "${prometheus_root}" \
    FAKE_PROMETHEUS_SMOKE_FAIL=1 2>&1); then
  fail 'candidate without authenticated Prometheus should fail the deployment'
fi
[[ $(cat "${prometheus_root}/state") == old ]] \
  || fail 'Prometheus smoke failure did not restore the previous image'
assert_contains "${prometheus_output}" 'prometheus_status=503'
assert_contains "${prometheus_output}" 'deployment outcome=rolled_back'

service_version_root=${WORK_DIR}/service-version
setup_fixture "${service_version_root}"
if service_version_output=$(run_update "${service_version_root}" \
    FAKE_CANDIDATE_SERVICE_VERSION_MISMATCH=1 2>&1); then
  fail 'candidate with the wrong service version should fail the deployment'
fi
[[ $(cat "${service_version_root}/state") == old ]] \
  || fail 'service-version mismatch did not restore the previous image'
assert_contains "${service_version_output}" \
  'deployment phase=readiness outcome=failure reason=service_version_mismatch'
assert_contains "${service_version_output}" 'deployment outcome=rolled_back'

restart_root=${WORK_DIR}/restart
setup_fixture "${restart_root}"
if restart_output=$(run_update "${restart_root}" FAKE_NEW_RESTART_COUNT=1 2>&1); then
  fail 'a restarted candidate should fail the deployment'
fi
[[ $(cat "${restart_root}/state") == old ]] \
  || fail 'restarted candidate did not restore the previous image'
assert_contains "${restart_output}" 'reason=restart count=1'
assert_contains "${restart_output}" 'deployment outcome=rolled_back'
assert_contract_unchanged "${restart_root}"

unknown_restart_root=${WORK_DIR}/unknown-restart
setup_fixture "${unknown_restart_root}"
if unknown_restart_output=$(run_update "${unknown_restart_root}" \
    FAKE_NEW_RESTART_COUNT=invalid 2>&1); then
  fail 'candidate with an unreadable restart count should fail the deployment'
fi
[[ $(cat "${unknown_restart_root}/state") == old ]] \
  || fail 'unreadable restart count did not restore the previous image'
assert_contains "${unknown_restart_output}" 'reason=timeout'
assert_contains "${unknown_restart_output}" 'deployment outcome=rolled_back'

closed_stderr_root=${WORK_DIR}/closed-stderr
setup_fixture "${closed_stderr_root}"
if run_update "${closed_stderr_root}" FAKE_PUBLIC_SMOKE_FAIL=1 \
    >/dev/null 2>&-; then
  fail 'candidate failure with a closed stderr should keep the deployment failed'
fi
[[ $(cat "${closed_stderr_root}/state") == old ]] \
  || fail 'closed stderr prevented automatic rollback'

closed_pipe_root=${WORK_DIR}/closed-pipe
setup_fixture "${closed_pipe_root}"
closed_pipe=${closed_pipe_root}/stderr.pipe
mkfifo "${closed_pipe}"
dd if="${closed_pipe}" of=/dev/null bs=1 count=0 >/dev/null 2>&1 &
closed_pipe_reader=$!
if run_update "${closed_pipe_root}" FAKE_PUBLIC_SMOKE_FAIL=1 \
    >/dev/null 2>"${closed_pipe}"; then
  fail 'candidate failure with a closed stderr pipe should keep deployment failed'
fi
wait "${closed_pipe_reader}" || true
[[ $(cat "${closed_pipe_root}/state") == old ]] \
  || fail 'SIGPIPE from a closed stderr reader prevented automatic rollback'

hup_closed_pipe_root=${WORK_DIR}/hup-closed-pipe
setup_fixture "${hup_closed_pipe_root}"
hup_closed_pipe=${hup_closed_pipe_root}/stderr.pipe
mkfifo "${hup_closed_pipe}"
dd if="${hup_closed_pipe}" of=/dev/null bs=1 count=0 >/dev/null 2>&1 &
hup_closed_pipe_reader=$!
if run_update "${hup_closed_pipe_root}" FAKE_SIGNAL_PARENT=HUP \
    >/dev/null 2>"${hup_closed_pipe}"; then
  fail 'HUP with a closed stderr pipe should keep deployment failed'
fi
wait "${hup_closed_pipe_reader}" || true
[[ $(cat "${hup_closed_pipe_root}/state") == old ]] \
  || fail 'HUP and SIGPIPE combination prevented automatic rollback'

for signal in HUP PIPE; do
  signal_root=${WORK_DIR}/signal-${signal}
  signal_output_file=${signal_root}/output.log
  setup_fixture "${signal_root}"
  # A command substitution captures output through a pipe. Some Bash/Linux
  # combinations start that subshell with SIGPIPE ignored, which is inherited
  # by the runner and makes the PIPE case test the harness instead of the trap.
  # A regular file keeps the signal disposition identical to an SSH session.
  if run_update "${signal_root}" FAKE_SIGNAL_PARENT="${signal}" \
      >"${signal_output_file}" 2>&1; then
    fail "${signal} during candidate recreation should fail the deployment"
  fi
  signal_output=$(<"${signal_output_file}")
  [[ $(cat "${signal_root}/state") == old ]] \
    || fail "${signal} during candidate recreation prevented automatic rollback"
  assert_contains "${signal_output}" 'deployment outcome=rolled_back'
done

diagnostic_failure_root=${WORK_DIR}/diagnostic-failure
setup_fixture "${diagnostic_failure_root}"
if diagnostic_failure_output=$(run_update "${diagnostic_failure_root}" \
    FAKE_NEW_RESTART_COUNT=1 FAKE_DIAGNOSTIC_MKTEMP_FAIL=1 2>&1); then
  fail 'candidate restart must keep the deployment failed when diagnostics cannot be created'
fi
[[ $(cat "${diagnostic_failure_root}/state") == old ]] \
  || fail 'diagnostic capture failure prevented exact rollback'
assert_contains "${diagnostic_failure_output}" \
  'deployment diagnostics unavailable reason=temporary_directory'
assert_contains "${diagnostic_failure_output}" 'deployment outcome=rolled_back'
assert_not_contains "${diagnostic_failure_output}" 'unsafe chmod target'

exited_root=${WORK_DIR}/exited
setup_fixture "${exited_root}"
if exited_output=$(run_update "${exited_root}" FAKE_NEW_STATE=exited 2>&1); then
  fail 'an exited candidate should fail the deployment'
fi
[[ $(cat "${exited_root}/state") == old ]] \
  || fail 'exited candidate did not restore the previous image'
assert_contains "${exited_output}" 'reason=stopped state=exited'
assert_contains "${exited_output}" 'deployment outcome=rolled_back'

rollback_failure_root=${WORK_DIR}/rollback-failure
setup_fixture "${rollback_failure_root}"
if rollback_failure_output=$(run_update "${rollback_failure_root}" \
    FAKE_NEW_RESTART_COUNT=1 FAKE_ROLLBACK_UP_FAIL=1 2>&1); then
  fail 'rollback failure must keep the deployment failed'
fi
assert_contains "${rollback_failure_output}" \
  'deployment outcome=rollback_failed operator_action=required'
[[ -f ${rollback_failure_root}/var/log/mapleland-deploy/last-candidate-failure/state.json ]] \
  || fail 'rollback failure overwrote or lost candidate diagnostics'
[[ -f ${rollback_failure_root}/var/log/mapleland-deploy/last-rollback-failure/state.json ]] \
  || fail 'rollback recreation failure did not preserve rollback diagnostics'

lock_root=${WORK_DIR}/lock
setup_fixture "${lock_root}"
if lock_output=$(run_update "${lock_root}" FAKE_FLOCK_EXIT=1 2>&1); then
  fail 'a concurrent deployment should be rejected'
fi
assert_contains "${lock_output}" 'deployment phase=lock outcome=busy'
[[ ! -s ${lock_root}/up.log ]] || fail 'busy deployment lock still recreated the service'

fifo_lock_root=${WORK_DIR}/fifo-lock
setup_fixture "${fifo_lock_root}"
mkdir -m 0700 "${fifo_lock_root}/run/mapleland-deploy"
mkfifo "${fifo_lock_root}/run/mapleland-deploy/mapleland-api.lock"
if fifo_lock_output=$(run_update "${fifo_lock_root}" 2>&1); then
  fail 'deployment should reject a pre-created FIFO lock'
fi
assert_contains "${fifo_lock_output}" \
  'deployment phase=lock outcome=failure reason=unsafe_lock_file'
[[ ! -s ${fifo_lock_root}/up.log ]] || fail 'unsafe FIFO lock reached service recreation'

symlink_lock_root=${WORK_DIR}/symlink-lock
setup_fixture "${symlink_lock_root}"
mkdir -m 0700 "${symlink_lock_root}/run/mapleland-deploy"
ln -s "${symlink_lock_root}/state" \
  "${symlink_lock_root}/run/mapleland-deploy/mapleland-api.lock"
if symlink_lock_output=$(run_update "${symlink_lock_root}" 2>&1); then
  fail 'deployment should reject a symlink lock'
fi
assert_contains "${symlink_lock_output}" \
  'deployment phase=lock outcome=failure reason=unsafe_lock_file'
[[ $(cat "${symlink_lock_root}/state") == old ]] \
  || fail 'symlink lock target was modified'

permissions_root=${WORK_DIR}/permissions
setup_fixture "${permissions_root}"
chmod 0644 "${permissions_root}/opt/mapleland/ghcr.env"
if permissions_output=$(run_update "${permissions_root}" 2>&1); then
  fail 'deployment should reject a world-readable credential file'
fi
assert_contains "${permissions_output}" \
  'deployment phase=inputs outcome=failure reason=unsafe_permissions'
[[ ! -s ${permissions_root}/up.log ]] \
  || fail 'unsafe credential permissions reached service recreation'

token_root=${WORK_DIR}/token
setup_fixture "${token_root}"
sed -i.bak 's/^MANAGEMENT_SCRAPE_TOKEN=.*/MANAGEMENT_SCRAPE_TOKEN=short/' \
  "${token_root}/opt/mapleland/.env"
rm "${token_root}/opt/mapleland/.env.bak"
if token_output=$(run_update "${token_root}" 2>&1); then
  fail 'deployment should reject a short management scrape token'
fi
assert_contains "${token_output}" \
  'deployment phase=inputs outcome=failure reason=invalid_management_token'

baseline_root=${WORK_DIR}/baseline
setup_fixture "${baseline_root}"
if baseline_output=$(run_update "${baseline_root}" FAKE_OLD_SMOKE_FAIL=1 2>&1); then
  fail 'deployment should not replace an unhealthy current service'
fi
assert_contains "${baseline_output}" \
  'deployment phase=preserve outcome=failure reason=baseline_unhealthy'
[[ ! -s ${baseline_root}/up.log ]] \
  || fail 'unhealthy baseline still recreated the service'
assert_contract_unchanged "${baseline_root}"

baseline_restart_root=${WORK_DIR}/baseline-restart
setup_fixture "${baseline_restart_root}"
if baseline_restart_output=$(run_update "${baseline_restart_root}" \
    FAKE_OLD_RESTART_COUNT=1 2>&1); then
  fail 'deployment should not replace a previously restarted service'
fi
assert_contains "${baseline_restart_output}" \
  'deployment phase=preserve outcome=failure reason=baseline_unhealthy'
assert_contains "${baseline_restart_output}" 'restart_count=1'
[[ ! -s ${baseline_restart_root}/up.log ]] \
  || fail 'restarted baseline still reached service recreation'

version_root=${WORK_DIR}/version
setup_fixture "${version_root}"
if version_output=$(run_update "${version_root}" \
    FAKE_OLD_SERVICE_VERSION_INVALID=1 2>&1); then
  fail 'deployment should not mutate when exact rollback service version is unavailable'
fi
assert_contains "${version_output}" \
  'deployment phase=preserve outcome=failure reason=service_version_unavailable'
[[ ! -s ${version_root}/up.log ]] \
  || fail 'missing rollback service version still recreated the service'
assert_contract_unchanged "${version_root}"

gateway_sudo=${WORK_DIR}/gateway-sudo
gateway_log=${WORK_DIR}/gateway.log
# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'printf "%s\n" "$*" >> "${FAKE_GATEWAY_LOG:?}"' \
  > "${gateway_sudo}"
chmod +x "${gateway_sudo}"

: > "${gateway_log}"
env SSH_ORIGINAL_COMMAND="deploy ${IMAGE_REF}" \
  MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
  MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
  FAKE_GATEWAY_LOG="${gateway_log}" \
  "${BASH}" "${UPDATE_SCRIPT}"
[[ $(cat "${gateway_log}") == "-n /opt/mapleland/update-api.sh ${IMAGE_REF}" ]] \
  || fail 'gateway did not dispatch the exact immutable deploy command'

assert_gateway_rejected() {
  local command=$1
  : > "${gateway_log}"
  if env SSH_ORIGINAL_COMMAND="${command}" \
      MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
      MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
      FAKE_GATEWAY_LOG="${gateway_log}" \
      "${BASH}" "${UPDATE_SCRIPT}" >/dev/null 2>&1; then
    fail "gateway accepted forbidden command: ${command}"
  fi
  [[ ! -s ${gateway_log} ]] || fail 'rejected gateway input reached sudo'
}

assert_gateway_rejected 'preflight'
assert_gateway_rejected 'deploy ghcr.io/team-maple/mls-be/mapleland-api:latest'
assert_gateway_rejected 'deploy ghcr.io/other/mls-be/mapleland-api@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
assert_gateway_rejected "deploy ${IMAGE_REF} extra"
assert_gateway_rejected "deploy ${IMAGE_REF}"$'\nuname -a'

: > "${gateway_log}"
if printf 'deploy %s\n' "${IMAGE_REF}" |
  env SSH_ORIGINAL_COMMAND='uname -a' \
    MAPLELAND_UPDATE_GATEWAY_TEST_MODE=1 \
    MAPLELAND_UPDATE_GATEWAY_SUDO_BIN="${gateway_sudo}" \
    FAKE_GATEWAY_LOG="${gateway_log}" \
    "${BASH}" "${UPDATE_SCRIPT}" >/dev/null 2>&1; then
  fail 'gateway should not accept an allowlisted command from stdin'
fi
[[ ! -s ${gateway_log} ]] || fail 'stdin bypass reached sudo'

if grep -Eq 'host-preflight|preflight-host|script:[[:space:]]+preflight' \
    "${DEPLOY_WORKFLOW}"; then
  fail 'deployment workflow still contains the removed preflight path'
fi
build_job=$(sed -n '/^  build-and-publish:/,/^  deploy:/p' "${DEPLOY_WORKFLOW}")
deploy_job=$(sed -n '/^  deploy:/,$p' "${DEPLOY_WORKFLOW}")
[[ $(grep -Ec '^    environment:$' <<< "${build_job}") == 1 \
  && $(grep -Ec '^      name: production-build$' <<< "${build_job}") == 1 \
  && $(grep -Ec '^      deployment: false$' <<< "${build_job}") == 1 ]] \
  || fail 'build secret is not isolated behind the main-only build environment'
[[ $(grep -Ec '^    environment:$' <<< "${deploy_job}") == 1 \
  && $(grep -Ec '^      name: production$' <<< "${deploy_job}") == 1 ]] \
  || fail 'host deployment is not protected by production approval'
[[ $(grep -Ec '^      packages: write$' <<< "${build_job}") == 1 \
  && $(grep -Ec '^      packages: write$' <<< "${deploy_job}") == 0 \
  && $(grep -Ec '^      id-token: write$' <<< "${DEPLOY_WORKFLOW}") == 0 ]] \
  || fail 'release jobs do not keep package and OIDC permissions least-privileged'
[[ $(grep -Ec '^    needs: build-and-publish$' <<< "${deploy_job}") == 1 ]] \
  || fail 'deploy job does not require the immutable build result'
[[ $(grep -Ec '^    timeout-minutes: 35$' <<< "${deploy_job}") == 1 ]] \
  || fail 'deploy job timeout does not reserve enough rollback time'
[[ $(grep -Ec '^    runs-on: ubuntu-24[.]04$' <<< "${deploy_job}") == 1 ]] \
  || fail 'deploy runner architecture is not fixed for the reviewed Tailscale archive'
if grep -Fq 'appleboy/ssh-action' <<< "${deploy_job}"; then
  fail 'deploy job still downloads an unverified SSH helper binary'
fi
grep -Fq 'name: Deploy with OpenSSH' <<< "${deploy_job}" \
  || fail 'deploy job does not use the runner OpenSSH client'
grep -Fq 'test -s "${known_hosts}"' <<< "${deploy_job}" \
  || fail 'SSH does not fail closed when the reviewed host key is absent'
grep -Fq 'StrictHostKeyChecking=yes' <<< "${deploy_job}" \
  || fail 'SSH strict host key checking is not enabled'
grep -Fq 'ubuntu@oracle-cloud "deploy ${IMAGE_REF}"' <<< "${deploy_job}" \
  || fail 'deployment workflow does not pass only the immutable image reference'
grep -Fq 'timeout --kill-after=15s 30m ssh' <<< "${deploy_job}" \
  || fail 'SSH timeout does not reserve enough rollback time'
grep -Fq 'version: 1.94.2' <<< "${deploy_job}" \
  || fail 'Tailscale binary version is not pinned'
grep -Fq 'sha256sum: c6f99a5d774c7783b56902188d69e9756fc3dddfb08ac6be4cb2585f3fecdc32' \
  <<< "${deploy_job}" \
  || fail 'Tailscale archive checksum is not pinned'
grep -Fq 'run: docker logout ghcr.io >/dev/null 2>&1 || true' "${DEPLOY_WORKFLOW}" \
  || fail 'build job does not clear its registry login before completion'
if grep -Eq '^[[:space:]]+uses: [^#[:space:]]+@v' "${DEPLOY_WORKFLOW}"; then
  fail 'deployment workflow contains a mutable action tag'
fi
grep -Fq -- '--env "BP_IMAGE_LABELS=org.opencontainers.image.revision=${GITHUB_SHA}"' \
  "${DEPLOY_WORKFLOW}" \
  || fail 'published image does not embed its immutable source revision'
grep -Fq 'PACK_ARCHIVE_SHA256: 2e9f46e422495d8ac7e5078b7f76cdca1c5f53d37783e18e69b7ff737ec440dc' \
  "${DEPLOY_WORKFLOW}" \
  || fail 'pack CLI archive checksum is not pinned'
[[ $(grep -Fc 'docker buildx imagetools inspect "${image_tag}"' \
  "${DEPLOY_WORKFLOW}") == 1 ]] \
  || fail 'published tag must be resolved to a digest exactly once'
grep -Fq 'image_ref="${PUBLISHED_IMAGE_REF}"' "${DEPLOY_WORKFLOW}" \
  || fail 'image verification does not use the resolved immutable digest'
grep -Fq -- '--builder paketobuildpacks/builder-noble-java-tiny@sha256:b57aaea99c5d33a944c925498411805d50245085f4515a781dab67abd3729a09' \
  "${DEPLOY_WORKFLOW}" \
  || fail 'Paketo builder is not pinned to the reviewed manifest digest'
grep -Fq -- '--run-image paketobuildpacks/ubuntu-noble-run-tiny@sha256:b30bdba17bde5b386cb2b364b69a794a79634b71b75e007789c38f6aba1b73b1' \
  "${DEPLOY_WORKFLOW}" \
  || fail 'Paketo runtime base is not pinned to the reviewed manifest digest'
grep -Fq 'published image does not use the reviewed run image' "${DEPLOY_WORKFLOW}" \
  || fail 'published image runtime base is not verified'
grep -Fq 'test "${image_revision}" = "${GITHUB_SHA}"' "${DEPLOY_WORKFLOW}" \
  || fail 'published image revision is not verified before deployment'
grep -Fxq 'distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78' \
  "${REPOSITORY_DIR}/gradle/wrapper/gradle-wrapper.properties" \
  || fail 'Gradle distribution checksum is not pinned'
grep -Fq 'timeout --kill-after=5s' "${UPDATE_SCRIPT}" \
  || fail 'runner commands are not bounded with a forced kill timeout'
if grep -Fq -- '--foreground' "${UPDATE_SCRIPT}"; then
  fail 'timeout foreground mode can leave Compose child processes running during rollback'
fi
[[ ! -e ${REPOSITORY_DIR}/.github/workflows/deploy-jar.yml ]] \
  || fail 'legacy mutable deployment workflow still exists'
[[ ! -e ${OBSERVABILITY_DIR}/preflight-host.sh ]] \
  || fail 'removed host preflight script still exists'
[[ ! -e ${TEST_DIR}/host-preflight-test.sh ]] \
  || fail 'removed host preflight test still exists'

echo 'update-api tests passed'
