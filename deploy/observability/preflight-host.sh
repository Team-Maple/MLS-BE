#!/usr/bin/env bash
set -euo pipefail

readonly EXPECTED_REPOSITORY=ghcr.io/team-maple/mls-be/mapleland-api
readonly EXPECTED_SHA256_REGEX='^[0-9a-f]{64}$'
readonly EXPECTED_SERVICE_VERSION_REGEX='^[0-9a-f]{40}$'

fail() {
  echo "host preflight failed: $*" >&2
  exit 1
}

if [[ ${MAPLELAND_PREFLIGHT_TEST_MODE:-0} == 1 ]]; then
  (( EUID != 0 )) || fail 'test mode is forbidden for root'
  readonly ROOT_PREFIX=${MAPLELAND_PREFLIGHT_TEST_ROOT:?test root is required}
  [[ ${ROOT_PREFIX} == /* && ${ROOT_PREFIX} != / ]] \
    || fail 'test root must be a non-root absolute path'
  readonly PREFLIGHT_FILE=${BASH_SOURCE[0]}
else
  (( EUID == 0 )) || fail 'preflight-host.sh must run as root'
  readonly ROOT_PREFIX=''
  readonly PREFLIGHT_FILE=/opt/mapleland/preflight-host.sh
fi

host_path() {
  printf '%s%s' "${ROOT_PREFIX}" "$1"
}

COMPOSE_FILE=$(host_path /opt/mapleland/docker-compose.yml)
readonly COMPOSE_FILE
APP_ENV_FILE=$(host_path /opt/mapleland/.env)
readonly APP_ENV_FILE
GHCR_ENV_FILE=$(host_path /opt/mapleland/ghcr.env)
readonly GHCR_ENV_FILE
UPDATE_FILE=$(host_path /opt/mapleland/update-api.sh)
readonly UPDATE_FILE
ALLOY_CONFIG_FILE=$(host_path /etc/alloy/config.alloy)
readonly ALLOY_CONFIG_FILE
ALLOY_ENV_FILE=$(host_path /etc/alloy/alloy.env)
readonly ALLOY_ENV_FILE
APP_LOG_DIR=$(host_path /var/log/mapleland-api)
readonly APP_LOG_DIR
APP_LOG_FILE=$(host_path /var/log/mapleland-api/mapleland-api.json)
readonly APP_LOG_FILE

if [[ $# -ne 2 ]]; then
  fail 'usage: preflight-host.sh <expected-preflight-sha256> <expected-update-sha256>'
fi
readonly expected_preflight_sha=$1
readonly expected_update_sha=$2
[[ ${expected_preflight_sha} =~ ${EXPECTED_SHA256_REGEX} ]] \
  || fail 'expected preflight checksum must be 64 lowercase hex characters'
[[ ${expected_update_sha} =~ ${EXPECTED_SHA256_REGEX} ]] \
  || fail 'expected update checksum must be 64 lowercase hex characters'

for required_command in awk bash curl docker getfacl grep id jq runuser sha256sum ss stat systemctl; do
  command -v "${required_command}" >/dev/null \
    || fail "required command is missing: ${required_command}"
done

require_root_file() {
  local path=$1
  local expected_mode=$2
  local label=$3

  [[ -f ${path} && ! -L ${path} ]] \
    || fail "${label} must be a regular, non-symlink file"
  [[ $(stat -c '%u' "${path}") == 0 && $(stat -c '%a' "${path}") == "${expected_mode}" ]] \
    || fail "${label} must be owned by root with mode 0${expected_mode}"
}

require_root_file "${PREFLIGHT_FILE}" 755 preflight-host.sh
require_root_file "${UPDATE_FILE}" 755 update-api.sh
require_root_file "${COMPOSE_FILE}" 640 docker-compose.yml
require_root_file "${APP_ENV_FILE}" 600 .env
require_root_file "${GHCR_ENV_FILE}" 600 ghcr.env
require_root_file "${ALLOY_CONFIG_FILE}" 640 config.alloy
require_root_file "${ALLOY_ENV_FILE}" 600 alloy.env

actual_preflight_sha=$(sha256sum "${PREFLIGHT_FILE}" | awk '{print $1}')
[[ ${actual_preflight_sha} == "${expected_preflight_sha}" ]] \
  || fail 'preflight-host.sh checksum mismatch; install the reviewed repository version before deployment'

actual_update_sha=$(sha256sum "${UPDATE_FILE}" | awk '{print $1}')
[[ ${actual_update_sha} == "${expected_update_sha}" ]] \
  || fail 'update-api.sh checksum mismatch; active host script is stale or unreviewed'
bash -n "${UPDATE_FILE}"

validate_ghcr_env() {
  local line
  local key
  local value
  local user_seen=false
  local token_seen=false

  while IFS= read -r line || [[ -n ${line} ]]; do
    case ${line} in
      ''|'#'*) continue ;;
    esac
    [[ ${line} == *=* ]] || fail 'ghcr.env contains an invalid line'
    key=${line%%=*}
    value=${line#*=}
    [[ -n ${value} && ${value} =~ ^[[:graph:]]+$ ]] \
      || fail 'ghcr.env values must be non-empty printable tokens'
    case ${key} in
      GHCR_USER)
        [[ ${user_seen} == false ]] || fail 'ghcr.env contains duplicate GHCR_USER'
        user_seen=true
        ;;
      GHCR_TOKEN)
        [[ ${token_seen} == false ]] || fail 'ghcr.env contains duplicate GHCR_TOKEN'
        token_seen=true
        ;;
      *) fail "ghcr.env contains an unexpected key: ${key}" ;;
    esac
  done < "${GHCR_ENV_FILE}"

  [[ ${user_seen} == true && ${token_seen} == true ]] \
    || fail 'ghcr.env must contain exactly one GHCR_USER and GHCR_TOKEN'
}

validate_app_env() {
  local line
  local key
  local value
  local management_token_seen=false
  local service_version_seen=false

  while IFS= read -r line || [[ -n ${line} ]]; do
    case ${line} in
      ''|'#'*) continue ;;
    esac
    [[ ${line} == *=* ]] || continue
    key=${line%%=*}
    value=${line#*=}
    case ${key} in
      MANAGEMENT_SCRAPE_TOKEN)
        [[ ${management_token_seen} == false ]] \
          || fail '.env contains duplicate MANAGEMENT_SCRAPE_TOKEN'
        [[ -n ${value} && ${#value} -ge 32 && ${value} =~ ^[[:graph:]]+$ ]] \
          || fail '.env MANAGEMENT_SCRAPE_TOKEN must be at least 32 printable characters'
        management_token_seen=true
        ;;
      SERVICE_VERSION)
        [[ ${service_version_seen} == false ]] \
          || fail '.env contains duplicate SERVICE_VERSION'
        [[ ${value} =~ ${EXPECTED_SERVICE_VERSION_REGEX} ]] \
          || fail '.env SERVICE_VERSION must be a full lowercase Git commit SHA'
        service_version_seen=true
        ;;
    esac
  done < "${APP_ENV_FILE}"

  [[ ${management_token_seen} == true && ${service_version_seen} == true ]] \
    || fail '.env must contain MANAGEMENT_SCRAPE_TOKEN and SERVICE_VERSION'
}

validate_ghcr_env
validate_app_env

docker compose -f "${COMPOSE_FILE}" config --quiet
compose_json=$(docker compose -f "${COMPOSE_FILE}" config --format json)
if ! printf '%s' "${compose_json}" | jq -e --arg repository "${EXPECTED_REPOSITORY}:" '
  .services["mapleland-api"] as $app |
  ($app | type == "object") and
  (($app.image | type) == "string" and ($app.image | startswith($repository))) and
  ($app.environment.MANAGEMENT_SERVER_ADDRESS == "0.0.0.0") and
  (($app.environment.MANAGEMENT_SERVER_PORT | tostring) == "18080") and
  (($app.environment.MANAGEMENT_SCRAPE_TOKEN | type) == "string") and
  (($app.environment.MANAGEMENT_SCRAPE_TOKEN | length) >= 32) and
  (($app.environment.SERVICE_VERSION | type) == "string") and
  ($app.environment.SERVICE_VERSION | test("^[0-9a-f]{40}$")) and
  ([ $app.ports[]? | select((.target | tostring) == "18080") ] | length == 1) and
  ([ $app.ports[]? |
      select((.target | tostring) == "18080" and
             (.published | tostring) == "18080" and
             .host_ip == "127.0.0.1" and
             (.protocol // "tcp") == "tcp") ] | length == 1) and
  (any($app.volumes[]?;
    .type == "bind" and
    .source == "/var/log/mapleland-api" and
    .target == "/workspace/logs" and
    .bind.create_host_path == false))
' >/dev/null; then
  compose_json=''
  fail 'Compose observability contract is incomplete or exposes the management boundary'
fi
compose_sha=$(printf '%s' "${compose_json}" | sha256sum | awk '{print $1}')
compose_json=''

[[ -d ${APP_LOG_DIR} && ! -L ${APP_LOG_DIR} ]] \
  || fail 'application log directory must be a non-symlink directory'
[[ $(stat -c '%u:%g %a' "${APP_LOG_DIR}") == '1002:1001 750' ]] \
  || fail 'application log directory must be owned by 1002:1001 with mode 0750'
acl=$(getfacl -cp "${APP_LOG_DIR}")
grep -Fx 'user:alloy:r-x' <<< "${acl}" >/dev/null \
  || fail 'application log directory is missing the Alloy access ACL'
grep -Fx 'default:user:alloy:r-x' <<< "${acl}" >/dev/null \
  || fail 'application log directory is missing the Alloy default ACL'
acl=''
runuser -u alloy -- test -r "${ALLOY_CONFIG_FILE}" \
  || fail 'Alloy cannot read its configuration'
if [[ -e ${APP_LOG_FILE} ]]; then
  [[ -f ${APP_LOG_FILE} && ! -L ${APP_LOG_FILE} ]] \
    || fail 'active application log must be a regular, non-symlink file'
  runuser -u alloy -- test -r "${APP_LOG_FILE}" \
    || fail 'Alloy cannot read the active application log'
fi

if id -nG alloy | grep -Eq '(^| )(adm|systemd-journal)( |$)'; then
  fail 'Alloy has an unnecessary broad log-reading group'
fi
systemctl is-active --quiet alloy.service \
  || fail 'Alloy service is not active'
[[ $(systemctl is-enabled alloy.service) == enabled ]] \
  || fail 'Alloy service is not enabled'
curl --fail --silent --show-error --max-time 5 \
  http://127.0.0.1:12345/-/ready >/dev/null \
  || fail 'Alloy readiness endpoint failed'

validate_exact_loopback_listener() {
  local port=$1
  local allow_absent=$2
  local sockets
  sockets=$(ss -H -lnt "sport = :${port}")
  if [[ -z ${sockets} ]]; then
    [[ ${allow_absent} == true ]] || fail "port ${port} has no listener"
    return 1
  fi
  if ! printf '%s\n' "${sockets}" | awk -v expected="127.0.0.1:${port}" '
    $4 == expected { exact++ }
    END { exit !(NR == 1 && exact == 1) }
  '; then
    if [[ ${port} == 18080 ]]; then
      fail 'management port must be absent or bound only to 127.0.0.1'
    fi
    fail "port ${port} must have exactly one IPv4 loopback listener"
  fi
  return 0
}

validate_exact_loopback_listener 12345 false
management_listener=absent
if validate_exact_loopback_listener 18080 true; then
  management_listener=loopback
  curl --fail --silent --show-error --max-time 5 \
    http://127.0.0.1:18080/actuator/health >/dev/null \
    || fail 'application management health endpoint failed'
fi

app_container_ids=$(docker compose -f "${COMPOSE_FILE}" ps -q mapleland-api)
[[ $(printf '%s\n' "${app_container_ids}" | grep -c .) -eq 1 ]] \
  || fail 'exactly one existing mapleland-api container is required'
readonly app_container_id=${app_container_ids}
app_state=$(docker inspect "${app_container_id}" --format '{{.State.Status}}')
[[ ${app_state} == running ]] || fail 'current mapleland-api container is not running'
current_image_id=$(docker inspect "${app_container_id}" --format '{{.Image}}')
[[ ${current_image_id} =~ ^sha256:[0-9a-f]{64}$ ]] \
  || fail 'current mapleland-api image ID is invalid'
docker image inspect "${current_image_id}" >/dev/null \
  || fail 'current mapleland-api image is unavailable for rollback'
curl --fail --silent --show-error --max-time 5 \
  http://127.0.0.1:8080/api/v1/jobs >/dev/null \
  || fail 'current public API smoke check failed'

attestation=$(printf '%s\n' \
  "${actual_preflight_sha}" \
  "${actual_update_sha}" \
  "${compose_sha}" \
  "${app_container_id}" \
  "${current_image_id}" \
  "${management_listener}" |
  sha256sum | awk '{print $1}')

printf 'host_preflight=ok attestation=%s current_image=%s management_listener=%s\n' \
  "${attestation}" "${current_image_id}" "${management_listener}"
