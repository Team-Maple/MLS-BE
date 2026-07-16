#!/usr/bin/env bash
set -euo pipefail

readonly GATEWAY_IMAGE_REGEX='ghcr[.]io/team-maple/mls-be/mapleland-api@sha256:[0-9a-f]{64}'

dispatch_forced_command() {
  local sudo_bin=$1
  local gateway_command=${SSH_ORIGINAL_COMMAND:-}
  local image_ref_from_gateway

  if [[ -z ${gateway_command} || ${#gateway_command} -gt 256 \
    || ${gateway_command} == *$'\n'* || ${gateway_command} == *$'\r'* ]]; then
    echo 'invalid CI deployment gateway command' >&2
    exit 1
  fi

  if [[ ${gateway_command} =~ ^deploy\ (${GATEWAY_IMAGE_REGEX})$ ]]; then
    image_ref_from_gateway=${BASH_REMATCH[1]}
    exec "${sudo_bin}" -n /opt/mapleland/update-api.sh "${image_ref_from_gateway}"
  fi

  echo 'CI deployment gateway command is not allowlisted' >&2
  exit 1
}

if [[ ${MAPLELAND_UPDATE_GATEWAY_TEST_MODE:-0} == 1 ]]; then
  if (( EUID == 0 )); then
    echo 'gateway test mode is forbidden for root' >&2
    exit 1
  fi
  readonly gateway_sudo_bin=${MAPLELAND_UPDATE_GATEWAY_SUDO_BIN:?gateway test sudo path is required}
  [[ ${gateway_sudo_bin} == /* && -x ${gateway_sudo_bin} ]]
  dispatch_forced_command "${gateway_sudo_bin}"
elif [[ ${MAPLELAND_UPDATE_TEST_MODE:-0} == 1 ]]; then
  if (( EUID == 0 )); then
    echo 'test mode is forbidden for root' >&2
    exit 1
  fi
  readonly ROOT_PREFIX=${MAPLELAND_UPDATE_TEST_ROOT:?test root is required}
  if [[ ${ROOT_PREFIX} != /* || ${ROOT_PREFIX} == / ]]; then
    echo 'test root must be a non-root absolute path' >&2
    exit 1
  fi
  readonly READINESS_TIMEOUT_SECONDS=2
  readonly SHORT_COMMAND_TIMEOUT_SECONDS=2
  readonly DEPLOY_COMMAND_TIMEOUT_SECONDS=5
  readonly EXPECTED_FILE_OWNER=${EUID}
elif (( EUID != 0 )); then
  dispatch_forced_command /usr/bin/sudo
else
  readonly ROOT_PREFIX=''
  readonly READINESS_TIMEOUT_SECONDS=90
  readonly SHORT_COMMAND_TIMEOUT_SECONDS=10
  readonly DEPLOY_COMMAND_TIMEOUT_SECONDS=180
  readonly EXPECTED_FILE_OWNER=0
fi

host_path() {
  printf '%s%s' "${ROOT_PREFIX}" "$1"
}

file_uid_mode() {
  local path=$1
  if stat -c '%u:%a' "${path}" >/dev/null 2>&1; then
    stat -c '%u:%a' "${path}"
  else
    stat -f '%u:%Lp' "${path}"
  fi
}

run_short() {
  timeout --kill-after=5s "${SHORT_COMMAND_TIMEOUT_SECONDS}s" "$@"
}

run_deploy_command() {
  timeout --kill-after=5s "${DEPLOY_COMMAND_TIMEOUT_SECONDS}s" "$@"
}

readonly EXPECTED_REPOSITORY=ghcr.io/team-maple/mls-be/mapleland-api
readonly EXPECTED_REPOSITORY_REGEX='ghcr[.]io/team-maple/mls-be/mapleland-api'
readonly PUBLIC_SMOKE_URL=http://127.0.0.1:8080/api/v1/jobs
readonly MANAGEMENT_SMOKE_URL=http://127.0.0.1:18080/actuator/info
readonly MANAGEMENT_PROMETHEUS_URL=http://127.0.0.1:18080/actuator/prometheus
COMPOSE_FILE=$(host_path /opt/mapleland/docker-compose.yml)
readonly COMPOSE_FILE
COMPOSE_OVERRIDE_FILE=$(host_path /opt/mapleland/docker-compose.observability.yml)
readonly COMPOSE_OVERRIDE_FILE
APP_ENV_FILE=$(host_path /opt/mapleland/.env)
readonly APP_ENV_FILE
GHCR_ENV_FILE=$(host_path /opt/mapleland/ghcr.env)
readonly GHCR_ENV_FILE
DEPLOY_RUNTIME_ROOT=$(host_path /run/mapleland-deploy)
readonly DEPLOY_RUNTIME_ROOT
DEPLOY_LOCK_FILE=${DEPLOY_RUNTIME_ROOT}/mapleland-api.lock
readonly DEPLOY_LOCK_FILE
DEPLOY_DIAGNOSTIC_ROOT=$(host_path /var/log/mapleland-deploy)
readonly DEPLOY_DIAGNOSTIC_ROOT

if [[ $# -ne 1 ]]; then
  echo 'usage: update-api.sh <immutable-image-reference>' >&2
  exit 1
fi

readonly image_ref=$1
if [[ ! ${image_ref} =~ ^${EXPECTED_REPOSITORY_REGEX}@sha256:[0-9a-f]{64}$ ]]; then
  echo 'refusing mutable or unexpected image reference' >&2
  exit 1
fi

if [[ -L ${DEPLOY_RUNTIME_ROOT} \
  || ( -e ${DEPLOY_RUNTIME_ROOT} && ! -d ${DEPLOY_RUNTIME_ROOT} ) ]]; then
  echo 'deployment phase=lock outcome=failure reason=unsafe_runtime_directory' >&2
  exit 1
fi
mkdir -p "${DEPLOY_RUNTIME_ROOT}"
chmod 0700 "${DEPLOY_RUNTIME_ROOT}"
if [[ $(file_uid_mode "${DEPLOY_RUNTIME_ROOT}") != "${EXPECTED_FILE_OWNER}:700" ]]; then
  echo 'deployment phase=lock outcome=failure reason=runtime_directory_permissions' >&2
  exit 1
fi
if [[ -L ${DEPLOY_LOCK_FILE} \
  || ( -e ${DEPLOY_LOCK_FILE} && ! -f ${DEPLOY_LOCK_FILE} ) ]]; then
  echo 'deployment phase=lock outcome=failure reason=unsafe_lock_file' >&2
  exit 1
fi
exec 9>"${DEPLOY_LOCK_FILE}"
chmod 0600 "${DEPLOY_LOCK_FILE}"
if ! flock -n 9; then
  echo 'deployment phase=lock outcome=busy' >&2
  exit 1
fi
echo 'deployment phase=lock outcome=success'

readonly -a COMPOSE_ARGS=(
  --env-file "${APP_ENV_FILE}"
  -f "${COMPOSE_FILE}"
  -f "${COMPOSE_OVERRIDE_FILE}"
)

require_trusted_file() {
  local path=$1
  local expected_mode=$2
  if [[ ! -f ${path} || -L ${path} ]]; then
    echo "deployment phase=inputs outcome=failure reason=unsafe_file path=${path}" >&2
    exit 1
  fi
  if [[ $(file_uid_mode "${path}") != "${EXPECTED_FILE_OWNER}:${expected_mode}" ]]; then
    echo "deployment phase=inputs outcome=failure reason=unsafe_permissions path=${path}" >&2
    exit 1
  fi
}

require_trusted_file "${COMPOSE_FILE}" 640
require_trusted_file "${COMPOSE_OVERRIDE_FILE}" 640
require_trusted_file "${APP_ENV_FILE}" 600
require_trusted_file "${GHCR_ENV_FILE}" 600

ghcr_user=''
ghcr_token=''
ghcr_user_seen=false
ghcr_token_seen=false
if [[ ! -r ${GHCR_ENV_FILE} ]]; then
  echo 'deployment phase=credentials outcome=failure reason=unreadable_file' >&2
  exit 1
fi
while IFS= read -r credential_line || [[ -n ${credential_line} ]]; do
  case ${credential_line} in
    ''|'#'*) continue ;;
  esac
  if [[ ${credential_line} != *=* ]]; then
    echo 'deployment phase=credentials outcome=failure reason=invalid_line' >&2
    exit 1
  fi
  credential_key=${credential_line%%=*}
  credential_value=${credential_line#*=}
  if [[ -z ${credential_value} || ! ${credential_value} =~ ^[[:graph:]]+$ ]]; then
    echo 'deployment phase=credentials outcome=failure reason=invalid_value' >&2
    exit 1
  fi
  case ${credential_key} in
    GHCR_USER)
      if [[ ${ghcr_user_seen} == true ]]; then
        echo 'deployment phase=credentials outcome=failure reason=duplicate_user' >&2
        exit 1
      fi
      ghcr_user=${credential_value}
      ghcr_user_seen=true
      ;;
    GHCR_TOKEN)
      if [[ ${ghcr_token_seen} == true ]]; then
        echo 'deployment phase=credentials outcome=failure reason=duplicate_token' >&2
        exit 1
      fi
      ghcr_token=${credential_value}
      ghcr_token_seen=true
      ;;
    *)
      echo 'deployment phase=credentials outcome=failure reason=unexpected_key' >&2
      exit 1
      ;;
  esac
done < "${GHCR_ENV_FILE}"
if [[ ${ghcr_user_seen} != true || ${ghcr_token_seen} != true ]]; then
  echo 'deployment phase=credentials outcome=failure reason=missing_key' >&2
  exit 1
fi
credential_line=''
credential_value=''

management_scrape_token=''
management_token_count=0
while IFS= read -r app_env_line || [[ -n ${app_env_line} ]]; do
  case ${app_env_line} in
    MANAGEMENT_SCRAPE_TOKEN=*)
      management_scrape_token=${app_env_line#*=}
      management_token_count=$((management_token_count + 1))
      ;;
  esac
done < "${APP_ENV_FILE}"
app_env_line=''
if (( management_token_count != 1 )) \
  || [[ ${#management_scrape_token} -lt 32 \
    || ! ${management_scrape_token} =~ ^[[:graph:]]+$ ]]; then
  echo 'deployment phase=inputs outcome=failure reason=invalid_management_token' >&2
  exit 1
fi

management_prometheus_status() {
  local escaped_token=${management_scrape_token//\\/\\\\}
  escaped_token=${escaped_token//\"/\\\"}
  printf 'header = "Authorization: Bearer %s"\n' "${escaped_token}" \
    | curl --config - --silent --output /dev/null --max-time 5 \
      --write-out '%{http_code}' "${MANAGEMENT_PROMETHEUS_URL}"
}

compose_image=''
rollback_tag=''
rollback_image_id=''
rollback_service_version=''
rollback_armed=false
docker_config_dir=''

capture_failure_diagnostics() (
  local container_id=$1
  local failure_role=$2
  if [[ ${failure_role} != candidate && ${failure_role} != rollback ]]; then
    return 1
  fi
  local diagnostic_dir=${DEPLOY_DIAGNOSTIC_ROOT}/last-${failure_role}-failure
  local diagnostic_tmp=''

  umask 077
  trap 'if [[ -n ${diagnostic_tmp} \
    && ${diagnostic_tmp} == "${DEPLOY_DIAGNOSTIC_ROOT}"/.last-${failure_role}-failure.* \
    && -d ${diagnostic_tmp} && ! -L ${diagnostic_tmp} ]]; then \
      rm -rf -- "${diagnostic_tmp}"; \
    fi' EXIT

  if [[ -L ${DEPLOY_DIAGNOSTIC_ROOT} \
    || ( -e ${DEPLOY_DIAGNOSTIC_ROOT} && ! -d ${DEPLOY_DIAGNOSTIC_ROOT} ) ]]; then
    echo 'deployment diagnostics unavailable reason=unsafe_root' >&2
    return 1
  fi
  if ! mkdir -p "${DEPLOY_DIAGNOSTIC_ROOT}" \
    || ! chmod 0700 "${DEPLOY_DIAGNOSTIC_ROOT}"; then
    echo 'deployment diagnostics unavailable reason=root_setup' >&2
    return 1
  fi
  if ! diagnostic_tmp=$(mktemp -d \
      "${DEPLOY_DIAGNOSTIC_ROOT}/.last-${failure_role}-failure.XXXXXX") \
    || [[ -z ${diagnostic_tmp} || ! -d ${diagnostic_tmp} \
      || -L ${diagnostic_tmp} \
      || ${diagnostic_tmp} != "${DEPLOY_DIAGNOSTIC_ROOT}"/.last-${failure_role}-failure.* ]]; then
    echo 'deployment diagnostics unavailable reason=temporary_directory' >&2
    return 1
  fi

  if ! run_short docker inspect "${container_id}" --format '{{json .State}}' \
      > "${diagnostic_tmp}/state.json"; then
    if ! printf '%s\n' '{"diagnostic":"container state unavailable"}' \
        > "${diagnostic_tmp}/state.json"; then
      return 1
    fi
  fi
  run_short docker logs --timestamps --tail 500 "${container_id}" \
    > "${diagnostic_tmp}/container.log" 2>&1 || true
  if ! printf 'captured_at=%s\ncontainer_id=%s\n' \
      "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${container_id}" \
      > "${diagnostic_tmp}/metadata" \
    || ! chmod 0600 \
      "${diagnostic_tmp}/state.json" \
      "${diagnostic_tmp}/container.log" \
      "${diagnostic_tmp}/metadata"; then
    return 1
  fi
  if [[ -L ${diagnostic_dir} \
    || ( -e ${diagnostic_dir} && ! -d ${diagnostic_dir} ) ]]; then
    echo 'deployment diagnostics unavailable reason=unsafe_target' >&2
    return 1
  fi
  if ! rm -rf -- "${diagnostic_dir}" \
    || ! mv "${diagnostic_tmp}" "${diagnostic_dir}"; then
    return 1
  fi
  diagnostic_tmp=''
  echo "deployment diagnostics=/var/log/mapleland-deploy/last-${failure_role}-failure" >&2
)

wait_for_readiness() {
  local expected_image_id=$1
  local expected_service_version=$2
  local failure_role=$3
  local deadline=$((SECONDS + READINESS_TIMEOUT_SECONDS))
  local candidate_container_id=''
  local observed_container_id
  local candidate_image_id
  local candidate_state
  local candidate_health
  local candidate_restart_count
  local candidate_service_version_line
  local public_status
  local management_status
  local prometheus_status

  while (( SECONDS < deadline )); do
    observed_container_id=$(run_short docker compose "${COMPOSE_ARGS[@]}" \
      ps --all -q mapleland-api 2>/dev/null || true)
    if [[ -n ${observed_container_id} ]]; then
      candidate_container_id=${observed_container_id}
    fi
    if [[ -n ${candidate_container_id} ]]; then
      candidate_restart_count=$(run_short docker inspect "${candidate_container_id}" \
        --format '{{.RestartCount}}' 2>/dev/null || true)
      if [[ ${candidate_restart_count} =~ ^[0-9]+$ ]] \
        && (( candidate_restart_count > 0 )); then
        capture_failure_diagnostics "${candidate_container_id}" "${failure_role}" || true
        echo "deployment phase=readiness outcome=failure reason=restart count=${candidate_restart_count}" >&2
        return 1
      fi

      candidate_state=$(run_short docker inspect "${candidate_container_id}" \
        --format '{{.State.Status}}' 2>/dev/null || true)
      if [[ ${candidate_state} == exited || ${candidate_state} == dead ]]; then
        capture_failure_diagnostics "${candidate_container_id}" "${failure_role}" || true
        echo "deployment phase=readiness outcome=failure reason=stopped state=${candidate_state}" >&2
        return 1
      fi

      candidate_image_id=$(run_short docker inspect "${candidate_container_id}" \
        --format '{{.Image}}' 2>/dev/null || true)
      candidate_health=$(run_short docker inspect "${candidate_container_id}" \
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
        2>/dev/null || true)
      candidate_service_version_line=$(run_short docker inspect "${candidate_container_id}" \
        --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null \
        | grep -E '^SERVICE_VERSION=[0-9a-f]{40}$' || true)
      if [[ ${candidate_image_id} == "${expected_image_id}" \
        && ${candidate_state} == running \
        && ${candidate_service_version_line} != "SERVICE_VERSION=${expected_service_version}" ]]; then
        capture_failure_diagnostics "${candidate_container_id}" "${failure_role}" || true
        echo 'deployment phase=readiness outcome=failure reason=service_version_mismatch' >&2
        return 1
      fi
      if [[ ${candidate_image_id} == "${expected_image_id}" \
        && ${candidate_state} == running \
        && ${candidate_restart_count} == 0 \
        && ( ${candidate_health} == none || ${candidate_health} == healthy ) ]]; then
        public_status=$(curl --silent --output /dev/null --max-time 5 \
          --write-out '%{http_code}' "${PUBLIC_SMOKE_URL}" || true)
        management_status=$(curl --silent --output /dev/null --max-time 5 \
          --write-out '%{http_code}' "${MANAGEMENT_SMOKE_URL}" || true)
        prometheus_status=$(management_prometheus_status || true)
        if [[ ${public_status} == 200 && ${management_status} == 200 \
          && ${prometheus_status} == 200 ]]; then
          echo "deployment phase=readiness outcome=success container=${candidate_container_id} image_id=${candidate_image_id}"
          return 0
        fi
      fi
    fi
    sleep 2
  done

  if [[ -n ${candidate_container_id} ]]; then
    capture_failure_diagnostics "${candidate_container_id}" "${failure_role}" || true
  fi
  public_status=$(curl --silent --output /dev/null --max-time 5 \
    --write-out '%{http_code}' "${PUBLIC_SMOKE_URL}" || true)
  management_status=$(curl --silent --output /dev/null --max-time 5 \
    --write-out '%{http_code}' "${MANAGEMENT_SMOKE_URL}" || true)
  prometheus_status=$(management_prometheus_status || true)
  echo "deployment phase=readiness outcome=failure reason=timeout seconds=${READINESS_TIMEOUT_SECONDS} public_status=${public_status:-000} management_status=${management_status:-000} prometheus_status=${prometheus_status:-000}" >&2
  return 1
}

rollback_deployment() {
  local rollback_container_id=''
  echo "deployment phase=rollback outcome=start image_id=${rollback_image_id}" >&2 || true
  run_short docker image tag "${rollback_tag}" "${compose_image}" || return 1
  export SERVICE_VERSION=${rollback_service_version}
  if ! run_deploy_command docker compose "${COMPOSE_ARGS[@]}" up \
      -d --no-deps --force-recreate --pull never mapleland-api; then
    rollback_container_id=$(run_short docker compose "${COMPOSE_ARGS[@]}" \
      ps --all -q mapleland-api 2>/dev/null || true)
    if [[ -n ${rollback_container_id} ]]; then
      capture_failure_diagnostics "${rollback_container_id}" rollback || true
    fi
    return 1
  fi
  wait_for_readiness "${rollback_image_id}" "${rollback_service_version}" rollback || return 1
  echo "deployment phase=rollback outcome=success image_id=${rollback_image_id}" >&2 || true
  return 0
}

cleanup() {
  local status=$?
  trap - EXIT
  trap '' INT TERM HUP PIPE
  set +e
  local rollback_outcome='not_armed'
  if (( status != 0 )) && [[ ${rollback_armed} == true ]]; then
    if rollback_deployment; then
      rollback_outcome='rolled_back'
    else
      rollback_outcome='rollback_failed'
      status=1
    fi
  fi
  if (( status != 0 )); then
    echo "deployment outcome=failure status=${status}" >&2 || true
    case ${rollback_outcome} in
      rolled_back) echo 'deployment outcome=rolled_back' >&2 || true ;;
      rollback_failed)
        echo 'deployment outcome=rollback_failed operator_action=required' >&2 || true
        ;;
    esac
  fi
  if [[ -n ${docker_config_dir} \
    && ${docker_config_dir} == "$(host_path /run)"/mapleland-docker-config.* ]]; then
    rm -rf -- "${docker_config_dir}"
  fi
  exit "${status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP
trap 'exit 141' PIPE

docker_config_dir=$(mktemp -d "$(host_path /run)/mapleland-docker-config.XXXXXX")
chmod 0700 "${docker_config_dir}"
export DOCKER_CONFIG=${docker_config_dir}

echo 'deployment phase=authenticate outcome=start registry=ghcr.io'
printf '%s' "${ghcr_token}" | run_short docker login ghcr.io \
  -u "${ghcr_user}" --password-stdin >/dev/null
ghcr_token=''
echo 'deployment phase=authenticate outcome=success registry=ghcr.io'

echo "deployment phase=pull outcome=start image=${image_ref}"
run_deploy_command docker pull "${image_ref}" >/dev/null
expected_image_id=$(run_short docker image inspect "${image_ref}" --format '{{.Id}}')
expected_service_version=$(run_short docker image inspect "${image_ref}" --format \
  '{{ index .Config.Labels "org.opencontainers.image.revision" }}')
if [[ ! ${expected_service_version} =~ ^[0-9a-f]{40}$ ]]; then
  echo 'deployment phase=pull outcome=failure reason=invalid_image_revision' >&2
  exit 1
fi
echo "deployment phase=pull outcome=success image_id=${expected_image_id} service_version=${expected_service_version}"

compose_image=$(run_short docker compose "${COMPOSE_ARGS[@]}" \
  config --images mapleland-api)
if [[ -z ${compose_image} || ${compose_image} == *$'\n'* \
  || ${compose_image} != "${EXPECTED_REPOSITORY}:"* ]]; then
  echo 'deployment phase=compose outcome=failure reason=application_image_invalid' >&2
  exit 1
fi

previous_container_id=$(run_short docker compose "${COMPOSE_ARGS[@]}" ps -q mapleland-api)
if [[ -z ${previous_container_id} ]]; then
  echo 'deployment phase=preserve outcome=failure reason=running_container_missing' >&2
  exit 1
fi
rollback_image_id=$(run_short docker inspect "${previous_container_id}" --format '{{.Image}}')
run_short docker image inspect "${rollback_image_id}" >/dev/null
if ! rollback_service_version_line=$(run_short docker inspect "${previous_container_id}" \
    --format '{{range .Config.Env}}{{println .}}{{end}}' \
    | grep -E '^SERVICE_VERSION=[0-9a-f]{40}$'); then
  echo 'deployment phase=preserve outcome=failure reason=service_version_unavailable' >&2
  exit 1
fi
if [[ ${rollback_service_version_line} == *$'\n'* ]]; then
  echo 'deployment phase=preserve outcome=failure reason=service_version_ambiguous' >&2
  exit 1
fi
if [[ ${rollback_service_version_line} =~ ^SERVICE_VERSION=([0-9a-f]{40})$ ]]; then
  rollback_service_version=${BASH_REMATCH[1]}
else
  echo 'deployment phase=preserve outcome=failure reason=service_version_unavailable' >&2
  exit 1
fi
rollback_service_version_line=''
current_state=$(run_short docker inspect "${previous_container_id}" \
  --format '{{.State.Status}}' 2>/dev/null || true)
current_restart_count=$(run_short docker inspect "${previous_container_id}" \
  --format '{{.RestartCount}}' 2>/dev/null || true)
current_health=$(run_short docker inspect "${previous_container_id}" \
  --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
  2>/dev/null || true)
current_public_status=$(curl --silent --output /dev/null --max-time 5 \
  --write-out '%{http_code}' "${PUBLIC_SMOKE_URL}" || true)
current_management_status=$(curl --silent --output /dev/null --max-time 5 \
  --write-out '%{http_code}' "${MANAGEMENT_SMOKE_URL}" || true)
current_prometheus_status=$(management_prometheus_status || true)
if [[ ${current_state} != running \
  || ${current_restart_count} != 0 \
  || ( ${current_health} != none && ${current_health} != healthy ) \
  || ${current_public_status} != 200 || ${current_management_status} != 200 \
  || ${current_prometheus_status} != 200 ]]; then
  echo "deployment phase=preserve outcome=failure reason=baseline_unhealthy state=${current_state:-unknown} restart_count=${current_restart_count:-unknown} health=${current_health:-unknown} public_status=${current_public_status:-000} management_status=${current_management_status:-000} prometheus_status=${current_prometheus_status:-000}" >&2
  exit 1
fi
rollback_stamp=$(date -u +%Y%m%dT%H%M%S%N)
rollback_image_short=${rollback_image_id#sha256:}
rollback_tag="${EXPECTED_REPOSITORY}:rollback-${rollback_stamp}-${rollback_image_short:0:12}-$$"
run_short docker image tag "${rollback_image_id}" "${rollback_tag}"
rollback_armed=true
echo "deployment phase=preserve outcome=success container=${previous_container_id} image_id=${rollback_image_id} service_version=${rollback_service_version} rollback_tag=${rollback_tag}"

run_short docker image tag "${image_ref}" "${compose_image}"
export SERVICE_VERSION=${expected_service_version}
echo "deployment phase=recreate outcome=start image_id=${expected_image_id}"
run_deploy_command docker compose "${COMPOSE_ARGS[@]}" up \
  -d --no-deps --force-recreate --pull never mapleland-api
echo 'deployment phase=recreate outcome=success'

wait_for_readiness "${expected_image_id}" "${expected_service_version}" candidate

rollback_armed=false
for diagnostic_dir in \
  "${DEPLOY_DIAGNOSTIC_ROOT}/last-candidate-failure" \
  "${DEPLOY_DIAGNOSTIC_ROOT}/last-rollback-failure"; do
  if [[ -d ${diagnostic_dir} && ! -L ${diagnostic_dir} ]]; then
    rm -rf -- "${diagnostic_dir}"
  fi
done
echo "deployment outcome=success image=${image_ref} image_id=${expected_image_id} telemetry_verification=external"
