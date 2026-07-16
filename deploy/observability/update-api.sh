#!/usr/bin/env bash
set -euo pipefail

readonly GATEWAY_SHA256_REGEX='[0-9a-f]{64}'
readonly GATEWAY_IMAGE_REGEX='ghcr[.]io/team-maple/mls-be/mapleland-api@sha256:[0-9a-f]{64}'

dispatch_forced_command() {
  local sudo_bin=$1
  local gateway_command
  local gateway_input
  local preflight_sha
  local update_sha
  local override_sha
  local image_ref_from_gateway

  gateway_input=${SSH_ORIGINAL_COMMAND:-}
  if [[ ${#gateway_input} -gt 512 ]]; then
    echo 'CI deployment gateway command is too long' >&2
    exit 1
  fi
  gateway_command=${gateway_input}
  if [[ -z ${gateway_command} \
    || ${gateway_command} == *$'\n'* || ${gateway_command} == *$'\r'* ]]; then
    echo 'invalid CI deployment gateway command' >&2
    exit 1
  fi

  if [[ ${gateway_command} =~ ^preflight\ (${GATEWAY_SHA256_REGEX})\ (${GATEWAY_SHA256_REGEX})\ (${GATEWAY_SHA256_REGEX})$ ]]; then
    preflight_sha=${BASH_REMATCH[1]}
    update_sha=${BASH_REMATCH[2]}
    override_sha=${BASH_REMATCH[3]}
    exec "${sudo_bin}" -n /opt/mapleland/preflight-host.sh \
      "${preflight_sha}" "${update_sha}" "${override_sha}"
  fi

  if [[ ${gateway_command} =~ ^deploy\ (${GATEWAY_SHA256_REGEX})\ (${GATEWAY_SHA256_REGEX})\ (${GATEWAY_SHA256_REGEX})\ (${GATEWAY_IMAGE_REGEX})$ ]]; then
    preflight_sha=${BASH_REMATCH[1]}
    update_sha=${BASH_REMATCH[2]}
    override_sha=${BASH_REMATCH[3]}
    image_ref_from_gateway=${BASH_REMATCH[4]}
    "${sudo_bin}" -n /opt/mapleland/preflight-host.sh \
      "${preflight_sha}" "${update_sha}" "${override_sha}"
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
elif (( EUID != 0 )); then
  dispatch_forced_command /usr/bin/sudo
else
  readonly ROOT_PREFIX=''
  readonly READINESS_TIMEOUT_SECONDS=90
fi

host_path() {
  printf '%s%s' "${ROOT_PREFIX}" "$1"
}

COMPOSE_FILE=$(host_path /opt/mapleland/docker-compose.yml)
readonly COMPOSE_FILE
COMPOSE_OVERRIDE_FILE=$(host_path /opt/mapleland/docker-compose.observability.yml)
readonly COMPOSE_OVERRIDE_FILE
APP_ENV_FILE=$(host_path /opt/mapleland/.env)
readonly APP_ENV_FILE
GHCR_ENV_FILE=$(host_path /opt/mapleland/ghcr.env)
readonly GHCR_ENV_FILE
readonly EXPECTED_REPOSITORY=ghcr.io/team-maple/mls-be/mapleland-api
readonly EXPECTED_REPOSITORY_REGEX='ghcr[.]io/team-maple/mls-be/mapleland-api'
readonly LEGACY_COMPOSE_ENV_REGEX='^[[:space:]]*-[[:space:]]*GRAFANA_CLOUD_(URL|USERNAME|PASSWORD)='
readonly LEGACY_APP_ENV_REGEX='^GRAFANA_CLOUD_(URL|USERNAME|PASSWORD)='
readonly MANAGEMENT_PROMETHEUS_URL=http://127.0.0.1:18080/actuator/prometheus
readonly PUBLIC_SMOKE_URL=http://127.0.0.1:8080/api/v1/jobs
DEPLOY_DIAGNOSTIC_ROOT=$(host_path /var/log/mapleland-deploy)
readonly DEPLOY_DIAGNOSTIC_ROOT

if [[ $# -ne 1 ]]; then
  echo "usage: update-api.sh <immutable-image-reference>" >&2
  exit 1
fi

readonly image_ref=$1
if [[ ! ${image_ref} =~ ^${EXPECTED_REPOSITORY_REGEX}@sha256:[0-9a-f]{64}$ ]]; then
  echo "refusing mutable or unexpected image reference" >&2
  exit 1
fi

for compose_contract_file in "${COMPOSE_FILE}" "${COMPOSE_OVERRIDE_FILE}"; do
  if [[ ! -f ${compose_contract_file} || -L ${compose_contract_file} ]]; then
    echo "${compose_contract_file} must be a regular, non-symlink file" >&2
    exit 1
  fi
  if [[ $(stat -c '%u' "${compose_contract_file}") != 0 ]] \
    || [[ $(stat -c '%a' "${compose_contract_file}") != 640 ]]; then
    echo "${compose_contract_file} must be owned by root with mode 0640" >&2
    exit 1
  fi
done

if [[ ! -f ${APP_ENV_FILE} || -L ${APP_ENV_FILE} ]]; then
  echo "${APP_ENV_FILE} must be a regular, non-symlink file" >&2
  exit 1
fi

if [[ $(stat -c '%u' "${APP_ENV_FILE}") != 0 ]] \
  || [[ $(stat -c '%a' "${APP_ENV_FILE}") != 600 ]]; then
  echo "${APP_ENV_FILE} must be owned by root with mode 0600" >&2
  exit 1
fi

management_scrape_token=''
management_token_count=0
recommendation_v1_engine=''
recommendation_v1_engine_count=0
recommendation_v2_enabled=''
recommendation_v2_enabled_count=0
recommendation_query_timeout_seconds=''
recommendation_query_timeout_count=0
while IFS= read -r app_env_line || [[ -n ${app_env_line} ]]; do
  case ${app_env_line} in
    MANAGEMENT_SCRAPE_TOKEN=*)
      management_scrape_token=${app_env_line#*=}
      management_token_count=$((management_token_count + 1))
      ;;
    RECOMMENDATION_V1_ENGINE=*)
      recommendation_v1_engine=${app_env_line#*=}
      recommendation_v1_engine_count=$((recommendation_v1_engine_count + 1))
      ;;
    RECOMMENDATION_V2_ENABLED=*)
      recommendation_v2_enabled=${app_env_line#*=}
      recommendation_v2_enabled_count=$((recommendation_v2_enabled_count + 1))
      ;;
    RECOMMENDATION_QUERY_TIMEOUT_SECONDS=*)
      recommendation_query_timeout_seconds=${app_env_line#*=}
      recommendation_query_timeout_count=$((recommendation_query_timeout_count + 1))
      ;;
  esac
done < "${APP_ENV_FILE}"
if (( management_token_count != 1 )) \
  || [[ -z ${management_scrape_token} || ${#management_scrape_token} -lt 32 \
    || ! ${management_scrape_token} =~ ^[[:graph:]]+$ ]]; then
  echo 'exactly one valid MANAGEMENT_SCRAPE_TOKEN is required' >&2
  exit 1
fi
readonly management_scrape_token
if (( recommendation_v1_engine_count != 1 )) \
  || [[ ! ${recommendation_v1_engine} =~ ^(AURA|MYSQL)$ ]]; then
  echo 'exactly one RECOMMENDATION_V1_ENGINE=AURA|MYSQL is required' >&2
  exit 1
fi
if (( recommendation_v2_enabled_count != 1 )) \
  || [[ ! ${recommendation_v2_enabled} =~ ^(true|false)$ ]]; then
  echo 'exactly one RECOMMENDATION_V2_ENABLED=true|false is required' >&2
  exit 1
fi
if (( recommendation_query_timeout_count != 1 )) \
  || [[ ! ${recommendation_query_timeout_seconds} =~ ^([1-9]|[1-5][0-9]|60)$ ]]; then
  echo 'exactly one recommendation query timeout between 1 and 60 is required' >&2
  exit 1
fi
readonly recommendation_v1_engine
readonly recommendation_v2_enabled
readonly recommendation_query_timeout_seconds

curl_management_prometheus() {
  local escaped_token=${management_scrape_token//\\/\\\\}
  escaped_token=${escaped_token//\"/\\\"}
  printf 'header = "Authorization: Bearer %s"\n' "${escaped_token}" |
    curl --config - "$@" "${MANAGEMENT_PROMETHEUS_URL}"
}

base_legacy_count=0
env_legacy_count=0
for legacy_key in URL USERNAME PASSWORD; do
  base_key_count=$(grep -Ec \
    "^[[:space:]]*-[[:space:]]*GRAFANA_CLOUD_${legacy_key}=" \
    "${COMPOSE_FILE}" || true)
  env_key_count=$(grep -Ec "^GRAFANA_CLOUD_${legacy_key}=" \
    "${APP_ENV_FILE}" || true)
  if (( base_key_count > 1 || env_key_count > 1 )); then
    echo "duplicate GRAFANA_CLOUD_${legacy_key} rollback entry" >&2
    exit 1
  fi
  base_legacy_count=$((base_legacy_count + base_key_count))
  env_legacy_count=$((env_legacy_count + env_key_count))
done
case "${base_legacy_count}:${env_legacy_count}" in
  0:0) rollback_uses_legacy_contract=false ;;
  3:3) rollback_uses_legacy_contract=true ;;
  *)
    echo 'legacy Grafana rollback environment must be either fully preserved or fully removed' >&2
    exit 1
    ;;
esac
readonly rollback_uses_legacy_contract

if [[ ! -f ${GHCR_ENV_FILE} || -L ${GHCR_ENV_FILE} ]]; then
  echo "${GHCR_ENV_FILE} must be a regular, non-symlink file" >&2
  exit 1
fi
if [[ $(stat -c '%u' "${GHCR_ENV_FILE}") != 0 ]] \
  || [[ $(stat -c '%a' "${GHCR_ENV_FILE}") != 600 ]]; then
  echo "${GHCR_ENV_FILE} must be owned by root with mode 0600" >&2
  exit 1
fi

ghcr_user=''
ghcr_token=''
ghcr_user_seen=false
ghcr_token_seen=false
while IFS= read -r credential_line || [[ -n ${credential_line} ]]; do
  case ${credential_line} in
    ''|'#'*)
      continue
      ;;
  esac

  if [[ ${credential_line} != *=* ]]; then
    echo "invalid credential line in ${GHCR_ENV_FILE}" >&2
    exit 1
  fi
  credential_key=${credential_line%%=*}
  credential_value=${credential_line#*=}
  if [[ -z ${credential_value} || ! ${credential_value} =~ ^[[:graph:]]+$ ]]; then
    echo "credential values must be non-empty printable tokens" >&2
    exit 1
  fi

  case ${credential_key} in
    GHCR_USER)
      if [[ ${ghcr_user_seen} == true ]]; then
        echo "duplicate GHCR_USER in ${GHCR_ENV_FILE}" >&2
        exit 1
      fi
      ghcr_user=${credential_value}
      ghcr_user_seen=true
      ;;
    GHCR_TOKEN)
      if [[ ${ghcr_token_seen} == true ]]; then
        echo "duplicate GHCR_TOKEN in ${GHCR_ENV_FILE}" >&2
        exit 1
      fi
      ghcr_token=${credential_value}
      ghcr_token_seen=true
      ;;
    *)
      echo "unexpected credential key in ${GHCR_ENV_FILE}: ${credential_key}" >&2
      exit 1
      ;;
  esac
done < "${GHCR_ENV_FILE}"

if [[ ${ghcr_user_seen} != true || ${ghcr_token_seen} != true ]]; then
  echo "GHCR_USER and GHCR_TOKEN are both required in ${GHCR_ENV_FILE}" >&2
  exit 1
fi
credential_line=''
credential_value=''

compose_image=''
rollback_tag=''
rollback_image_id=''
rollback_armed=false
compose_env_file=''
compose_candidate_file=''
COMPOSE_ARGS=()

capture_failure_diagnostics() (
  local container_id=$1
  local diagnostic_dir=${DEPLOY_DIAGNOSTIC_ROOT}/last-failure
  local diagnostic_tmp

  umask 077
  trap 'if [[ -n ${diagnostic_tmp:-} \
    && ${diagnostic_tmp} == "${DEPLOY_DIAGNOSTIC_ROOT}"/.last-failure.* \
    && -d ${diagnostic_tmp} && ! -L ${diagnostic_tmp} ]]; then \
      rm -rf -- "${diagnostic_tmp}"; \
    fi' EXIT

  if [[ -L ${DEPLOY_DIAGNOSTIC_ROOT} \
    || ( -e ${DEPLOY_DIAGNOSTIC_ROOT} && ! -d ${DEPLOY_DIAGNOSTIC_ROOT} ) ]]; then
    echo 'deployment diagnostic root is not a safe directory' >&2
    return 1
  fi
  mkdir -p "${DEPLOY_DIAGNOSTIC_ROOT}" \
    || { echo 'could not create deployment diagnostic root' >&2; return 1; }
  chown root:root "${DEPLOY_DIAGNOSTIC_ROOT}" \
    || { echo 'could not own deployment diagnostic root' >&2; return 1; }
  chmod 0700 "${DEPLOY_DIAGNOSTIC_ROOT}" \
    || { echo 'could not protect deployment diagnostic root' >&2; return 1; }

  if [[ -L ${diagnostic_dir} \
    || ( -e ${diagnostic_dir} && ! -d ${diagnostic_dir} ) ]]; then
    echo 'deployment diagnostic target is not a safe directory' >&2
    return 1
  fi

  diagnostic_tmp=$(mktemp -d "${DEPLOY_DIAGNOSTIC_ROOT}/.last-failure.XXXXXX") \
    || { echo 'could not create temporary deployment diagnostics' >&2; return 1; }
  chown root:root "${diagnostic_tmp}" \
    || { echo 'could not own temporary deployment diagnostics' >&2; return 1; }
  chmod 0700 "${diagnostic_tmp}" \
    || { echo 'could not protect temporary deployment diagnostics' >&2; return 1; }

  if ! docker inspect "${container_id}" --format '{{json .State}}' \
    > "${diagnostic_tmp}/state.json"; then
    printf '%s\n' '{"diagnostic":"container state unavailable"}' \
      > "${diagnostic_tmp}/state.json" \
      || { echo 'could not write fallback container state' >&2; return 1; }
  fi
  docker logs --timestamps --tail 500 "${container_id}" \
    > "${diagnostic_tmp}/container.log" 2>&1 || true
  printf 'captured_at=%s\ncontainer_id=%s\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${container_id}" \
    > "${diagnostic_tmp}/metadata" \
    || { echo 'could not write deployment diagnostic metadata' >&2; return 1; }
  chmod 0600 \
    "${diagnostic_tmp}/state.json" \
    "${diagnostic_tmp}/container.log" \
    "${diagnostic_tmp}/metadata" \
    || { echo 'could not protect deployment diagnostic files' >&2; return 1; }
  chown root:root \
    "${diagnostic_tmp}/state.json" \
    "${diagnostic_tmp}/container.log" \
    "${diagnostic_tmp}/metadata" \
    || { echo 'could not own deployment diagnostic files' >&2; return 1; }

  rm -rf -- "${diagnostic_dir}" \
    || { echo 'could not replace prior deployment diagnostics' >&2; return 1; }
  mv "${diagnostic_tmp}" "${diagnostic_dir}" \
    || { echo 'could not publish deployment diagnostics' >&2; return 1; }
  diagnostic_tmp=''
  echo 'candidate diagnostics saved to /var/log/mapleland-deploy/last-failure' >&2
)

wait_for_readiness() {
  local expected_image_id=$1
  local require_management_health=${2:-true}
  local deadline=$((SECONDS + READINESS_TIMEOUT_SECONDS))
  local candidate_container_id=''
  local observed_container_id
  local candidate_image_id
  local candidate_state
  local candidate_health
  local candidate_restart_count
  local candidate_environment
  local management_status
  local public_status

  while (( SECONDS < deadline )); do
    observed_container_id=$(docker compose "${COMPOSE_ARGS[@]}" \
      ps --all -q mapleland-api)
    if [[ -n ${observed_container_id} ]]; then
      candidate_container_id=${observed_container_id}
    fi
    if [[ -n ${candidate_container_id} ]]; then
      candidate_restart_count=$(docker inspect "${candidate_container_id}" \
        --format '{{.RestartCount}}')
      if [[ ${candidate_restart_count} =~ ^[0-9]+$ ]] \
        && (( candidate_restart_count > 0 )); then
        capture_failure_diagnostics "${candidate_container_id}" || true
        echo "candidate entered a restart loop (restart_count=${candidate_restart_count})" >&2
        return 1
      fi
      candidate_image_id=$(docker inspect "${candidate_container_id}" --format '{{.Image}}')
      candidate_state=$(docker inspect "${candidate_container_id}" --format '{{.State.Status}}')
      if [[ ${candidate_state} == exited || ${candidate_state} == dead ]]; then
        capture_failure_diagnostics "${candidate_container_id}" || true
        echo "candidate stopped before readiness (state=${candidate_state})" >&2
        return 1
      fi
      candidate_health=$(docker inspect "${candidate_container_id}" \
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}')

      if [[ ${candidate_image_id} == "${expected_image_id}" \
        && ${candidate_state} == running \
        && ( ${candidate_health} == none || ${candidate_health} == healthy ) ]]; then
        if [[ ${require_management_health} == true ]]; then
          candidate_environment=$(docker inspect "${candidate_container_id}" \
            --format '{{range .Config.Env}}{{println .}}{{end}}')
          if grep -Eq '^GRAFANA_CLOUD_(URL|USERNAME|PASSWORD)=' \
              <<< "${candidate_environment}" \
            || ! grep -Fxq "RECOMMENDATION_V1_ENGINE=${recommendation_v1_engine}" \
              <<< "${candidate_environment}" \
            || ! grep -Fxq "RECOMMENDATION_V2_ENABLED=${recommendation_v2_enabled}" \
              <<< "${candidate_environment}" \
            || ! grep -Fxq \
              "RECOMMENDATION_QUERY_TIMEOUT_SECONDS=${recommendation_query_timeout_seconds}" \
              <<< "${candidate_environment}"; then
            candidate_environment=''
            sleep 2
            continue
          fi
          candidate_environment=''
        fi
        if [[ ${require_management_health} == false ]] \
          || curl_management_prometheus \
            --fail --silent --max-time 5 >/dev/null; then
          if curl --fail --silent --max-time 5 "${PUBLIC_SMOKE_URL}" >/dev/null; then
            return 0
          fi
        fi
      fi
    fi
    sleep 2
  done

  if [[ -n ${candidate_container_id} ]]; then
    capture_failure_diagnostics "${candidate_container_id}" || true
  fi
  management_status=$(curl_management_prometheus \
    --silent --output /dev/null --max-time 5 --write-out '%{http_code}' || true)
  public_status=$(curl --silent --output /dev/null --max-time 5 \
    --write-out '%{http_code}' "${PUBLIC_SMOKE_URL}" || true)
  echo "mapleland-api did not become ready within ${READINESS_TIMEOUT_SECONDS}s" >&2
  printf 'readiness diagnostics: management_prometheus_status=%s public_smoke_status=%s\n' \
    "${management_status:-000}" "${public_status:-000}" >&2
  return 1
}

rollback_deployment() {
  echo "deployment failed; rolling back to ${rollback_tag}" >&2
  docker image tag "${rollback_tag}" "${compose_image}" || return 1
  if [[ ${rollback_uses_legacy_contract} == true ]]; then
    docker compose -f "${COMPOSE_FILE}" up \
      -d --no-deps --force-recreate --pull never mapleland-api || return 1
  else
    docker compose "${COMPOSE_ARGS[@]}" up \
      -d --no-deps --force-recreate --pull never mapleland-api || return 1
  fi
  wait_for_readiness "${rollback_image_id}" false || return 1
  echo "mapleland-api rollback completed with exact previous image" >&2
}

cleanup() {
  local status=$?
  trap - EXIT INT TERM
  if (( status != 0 )) && [[ ${rollback_armed} == true ]]; then
    if ! rollback_deployment; then
      echo "automatic rollback failed; operator intervention is required" >&2
      status=1
    fi
  fi
  if [[ -n ${compose_env_file} ]]; then
    rm -f "${compose_env_file}"
  fi
  if [[ -n ${compose_candidate_file} ]]; then
    rm -f "${compose_candidate_file}"
  fi
  docker logout ghcr.io >/dev/null 2>&1 || true
  exit "${status}"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

compose_env_file=$(mktemp "$(host_path /run)/mapleland-compose-env.XXXXXX")
chmod 0600 "${compose_env_file}"
awk -v regex="${LEGACY_APP_ENV_REGEX}" '$0 !~ regex' \
  "${APP_ENV_FILE}" > "${compose_env_file}"
compose_candidate_file=$(mktemp "$(host_path /opt/mapleland)/.docker-compose.deploy.XXXXXX")
chmod 0640 "${compose_candidate_file}"
awk -v regex="${LEGACY_COMPOSE_ENV_REGEX}" '$0 !~ regex' \
  "${COMPOSE_FILE}" > "${compose_candidate_file}"
chown root:root "${compose_env_file}" "${compose_candidate_file}"
COMPOSE_ARGS=(
  --env-file "${compose_env_file}"
  -f "${compose_candidate_file}"
  -f "${COMPOSE_OVERRIDE_FILE}"
)
readonly -a COMPOSE_ARGS

printf '%s' "${ghcr_token}" | docker login ghcr.io -u "${ghcr_user}" --password-stdin
ghcr_token=''
docker pull "${image_ref}"
expected_image_id=$(docker image inspect "${image_ref}" --format '{{.Id}}')

mapfile -t configured_images < <(docker compose "${COMPOSE_ARGS[@]}" config --images)
for configured_image in "${configured_images[@]}"; do
  if [[ ${configured_image} == "${EXPECTED_REPOSITORY}:"* ]]; then
    if [[ -n ${compose_image} ]]; then
      echo "multiple mapleland-api image references found in Compose configuration" >&2
      exit 1
    fi
    compose_image=${configured_image}
  fi
done
: "${compose_image:?mapleland-api image is missing from Compose configuration}"

previous_container_id=$(docker compose "${COMPOSE_ARGS[@]}" ps -q mapleland-api)
if [[ -z ${previous_container_id} ]]; then
  echo "a running mapleland-api container is required for safe rollback" >&2
  exit 1
fi
rollback_image_id=$(docker inspect "${previous_container_id}" --format '{{.Image}}')
docker image inspect "${rollback_image_id}" >/dev/null
rollback_stamp=$(date -u +%Y%m%dT%H%M%S%N)
rollback_image_short=${rollback_image_id#sha256:}
rollback_tag="${EXPECTED_REPOSITORY}:rollback-${rollback_stamp}-${rollback_image_short:0:12}-$$"
docker image tag "${rollback_image_id}" "${rollback_tag}"
rollback_armed=true
echo "previous image preserved as ${rollback_tag}"

# Keep the current Compose contract while making its local tag point at the exact
# digest-pinned image. --pull never prevents a concurrent registry resolution.
docker image tag "${image_ref}" "${compose_image}"
docker compose "${COMPOSE_ARGS[@]}" up \
  -d --no-deps --force-recreate --pull never mapleland-api

wait_for_readiness "${expected_image_id}" true

remove_legacy_appender_environment() {
  if [[ ${base_legacy_count} == 0 ]]; then
    rollback_armed=false
    return 0
  fi

  # The new image is healthy and has no legacy credentials. Do not turn a
  # subsequent local config-cleanup error into an unnecessary app rollback.
  rollback_armed=false
  if ! mv "${compose_env_file}" "${APP_ENV_FILE}"; then
    return 1
  fi
  compose_env_file=''
  if ! mv "${compose_candidate_file}" "${COMPOSE_FILE}"; then
    return 1
  fi
  compose_candidate_file=''
}

remove_legacy_appender_environment

echo "mapleland-api is running the requested immutable image"
