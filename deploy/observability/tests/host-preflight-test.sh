#!/usr/bin/env bash
set -euo pipefail
# The fixture builders intentionally write literal shell expansions into mock
# executables; those values must expand only when each mock runs.

TEST_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
readonly TEST_DIR
OBSERVABILITY_DIR=$(cd "${TEST_DIR}/.." && pwd)
readonly OBSERVABILITY_DIR
readonly PREFLIGHT_SCRIPT=${OBSERVABILITY_DIR}/preflight-host.sh
readonly UPDATE_SCRIPT=${OBSERVABILITY_DIR}/update-api.sh
readonly COMPOSE_OVERRIDE_SOURCE=${OBSERVABILITY_DIR}/docker-compose.override.example.yml
readonly FIXTURE_VERSION=f4bf228b934959be125a72540c91e43f003b7b6e

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_contains() {
  local haystack=$1
  local needle=$2
  [[ ${haystack} == *"${needle}"* ]] \
    || fail "expected output to contain: ${needle}"
}

assert_not_contains() {
  local haystack=$1
  local needle=$2
  [[ ${haystack} != *"${needle}"* ]] \
    || fail "output exposed protected fixture value: ${needle}"
}

TMP_DIR=$(mktemp -d)
readonly TMP_DIR
trap 'rm -rf "${TMP_DIR}"' EXIT
readonly ROOT_DIR=${TMP_DIR}/root
readonly FAKE_BIN=${TMP_DIR}/bin

mkdir -p \
  "${ROOT_DIR}/opt/mapleland" \
  "${ROOT_DIR}/etc/alloy" \
  "${ROOT_DIR}/var/log/mapleland-api" \
  "${FAKE_BIN}"

cp "${UPDATE_SCRIPT}" "${ROOT_DIR}/opt/mapleland/update-api.sh"
cp "${COMPOSE_OVERRIDE_SOURCE}" \
  "${ROOT_DIR}/opt/mapleland/docker-compose.observability.yml"
printf '%s\n' \
  'GHCR_USER=fixture-user' \
  'GHCR_TOKEN=fixture-ghcr-secret' \
  > "${ROOT_DIR}/opt/mapleland/ghcr.env"
printf '%s\n' \
  'MANAGEMENT_SCRAPE_TOKEN=fixture-management-secret-0123456789' \
  "SERVICE_VERSION=${FIXTURE_VERSION}" \
  'RECOMMENDATION_V1_ENGINE=AURA' \
  'RECOMMENDATION_V2_ENABLED=false' \
  'RECOMMENDATION_QUERY_TIMEOUT_SECONDS=10' \
  > "${ROOT_DIR}/opt/mapleland/.env"
printf '%s\n' 'services:' '  mapleland-api:' \
  '    image: ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64' \
  > "${ROOT_DIR}/opt/mapleland/docker-compose.yml"
printf '%s\n' 'fixture alloy config' > "${ROOT_DIR}/etc/alloy/config.alloy"
printf '%s\n' 'fixture alloy env' > "${ROOT_DIR}/etc/alloy/alloy.env"
printf '%s\n' '{"message":"fixture"}' \
  > "${ROOT_DIR}/var/log/mapleland-api/mapleland-api.json"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'format=${2:-}' \
  'path=${3:-}' \
  'case "${format}" in' \
  "  '%u') printf '%s\\n' 0 ;;" \
  "  '%g')" \
  "    if [[ \${path} == */var/log/mapleland-api ]]; then printf '%s\\n' 1001; else printf '%s\\n' 0; fi ;;" \
  "  '%a')" \
  "    case \${path} in" \
  "      */ghcr.env) printf '%s\\n' \"\${FAKE_GHCR_MODE:-600}\" ;;" \
  "      */.env|*/alloy.env) printf '%s\\n' 600 ;;" \
  "      */docker-compose.yml|*/docker-compose.observability.yml|*/config.alloy) printf '%s\\n' 640 ;;" \
  "      */update-api.sh|*/preflight-host.sh) printf '%s\\n' 755 ;;" \
  "      *) printf '%s\\n' 750 ;;" \
  '    esac ;;' \
  "  '%u:%g %a')" \
  "    if [[ \${path} == */var/log/mapleland-api ]]; then printf '%s\\n' '1002:1001 750'; else printf '%s\\n' '0:0 600'; fi ;;" \
  '  *) echo "unsupported fake stat format: ${format}" >&2; exit 2 ;;' \
  'esac' \
  > "${FAKE_BIN}/stat"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'if [[ ${1:-} == compose ]]; then' \
  '  shift' \
  '  while [[ ${1:-} == -f || ${1:-} == --env-file ]]; do shift 2; done' \
  '  case ${1:-} in' \
  '    config)' \
  '      if [[ ${2:-} == --quiet ]]; then exit 0; fi' \
  '      if [[ ${2:-} == --format && ${3:-} == json ]]; then' \
  "        if [[ \${FAKE_LEGACY_GRAFANA_ENV:-0} == 1 ]]; then" \
  "          printf '%s\\n' '{\"services\":{\"mapleland-api\":{\"image\":\"ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64\",\"environment\":{\"MANAGEMENT_SERVER_ADDRESS\":\"0.0.0.0\",\"MANAGEMENT_SERVER_PORT\":\"18080\",\"MANAGEMENT_SCRAPE_TOKEN\":\"fixture-management-secret-0123456789\",\"SERVICE_VERSION\":\"f4bf228b934959be125a72540c91e43f003b7b6e\",\"RECOMMENDATION_V1_ENGINE\":\"AURA\",\"RECOMMENDATION_V2_ENABLED\":\"false\",\"RECOMMENDATION_QUERY_TIMEOUT_SECONDS\":\"10\",\"GRAFANA_CLOUD_PASSWORD\":\"fixture-legacy-secret\"},\"ports\":[{\"host_ip\":\"127.0.0.1\",\"target\":18080,\"published\":\"18080\",\"protocol\":\"tcp\"}],\"volumes\":[{\"type\":\"bind\",\"source\":\"/var/log/mapleland-api\",\"target\":\"/workspace/logs\",\"bind\":{\"create_host_path\":false}}]}}}'" \
  "        elif [[ \${FAKE_COMPOSE_VALID:-1} == 1 ]]; then" \
  "          printf '%s\\n' '{\"services\":{\"mapleland-api\":{\"image\":\"ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64\",\"environment\":{\"MANAGEMENT_SERVER_ADDRESS\":\"0.0.0.0\",\"MANAGEMENT_SERVER_PORT\":\"18080\",\"MANAGEMENT_SCRAPE_TOKEN\":\"fixture-management-secret-0123456789\",\"SERVICE_VERSION\":\"f4bf228b934959be125a72540c91e43f003b7b6e\",\"RECOMMENDATION_V1_ENGINE\":\"AURA\",\"RECOMMENDATION_V2_ENABLED\":\"false\",\"RECOMMENDATION_QUERY_TIMEOUT_SECONDS\":\"10\"},\"ports\":[{\"host_ip\":\"127.0.0.1\",\"target\":18080,\"published\":\"18080\",\"protocol\":\"tcp\"}],\"volumes\":[{\"type\":\"bind\",\"source\":\"/var/log/mapleland-api\",\"target\":\"/workspace/logs\",\"bind\":{}}]}}}'" \
  "        else" \
  "          printf '%s\\n' '{\"services\":{\"mapleland-api\":{\"image\":\"ghcr.io/team-maple/mls-be/mapleland-api:latest-arm64\",\"environment\":{},\"ports\":[{\"host_ip\":\"0.0.0.0\",\"target\":18080,\"published\":\"18080\"}],\"volumes\":[]}}}'" \
  "        fi" \
  "        exit 0" \
  '      fi ;;' \
  '    ps) printf "%s\\n" fixture-container-id; exit 0 ;;' \
  '  esac' \
  'elif [[ ${1:-} == inspect ]]; then' \
  '  case $* in' \
  '    *State.Status*) printf "%s\\n" running ;;' \
  '    *.Image*) printf "%s\\n" sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa ;;' \
  '    *) exit 2 ;;' \
  '  esac' \
  '  exit 0' \
  'elif [[ ${1:-} == image && ${2:-} == inspect ]]; then' \
  '  exit 0' \
  'fi' \
  'echo "unsupported fake docker invocation: $*" >&2' \
  'exit 2' \
  > "${FAKE_BIN}/docker"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'case ${1:-} in' \
  '  is-active) exit 0 ;;' \
  '  is-enabled) printf "%s\\n" enabled ;;' \
  '  *) exit 2 ;;' \
  'esac' \
  > "${FAKE_BIN}/systemctl"

printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${FAKE_BIN}/curl"
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "${FAKE_BIN}/runuser"
# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'if [[ ${1:-} == -nG && ${2:-} == alloy ]]; then printf "%s\\n" alloy; else /usr/bin/id "$@"; fi' \
  > "${FAKE_BIN}/id"

# shellcheck disable=SC2016
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'case $* in' \
  '  *:12345*) printf "%s\\n" "LISTEN 0 4096 127.0.0.1:12345 0.0.0.0:*" ;;' \
  '  *:18080*)' \
  '    if [[ ${FAKE_MANAGEMENT_LISTENER:-absent} == wildcard ]]; then' \
  '      printf "%s\\n" "LISTEN 0 4096 0.0.0.0:18080 0.0.0.0:*"' \
  '    elif [[ ${FAKE_MANAGEMENT_LISTENER:-absent} == loopback ]]; then' \
  '      printf "%s\\n" "LISTEN 0 4096 127.0.0.1:18080 0.0.0.0:*"' \
  '    fi ;;' \
  '  *) exit 2 ;;' \
  'esac' \
  > "${FAKE_BIN}/ss"

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'printf "%s\\n" "user:alloy:r-x" "default:user:alloy:r-x"' \
  > "${FAKE_BIN}/getfacl"

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'if (($#)); then exec shasum -a 256 "$@"; else exec shasum -a 256; fi' \
  > "${FAKE_BIN}/sha256sum"

chmod +x "${FAKE_BIN}"/*

run_preflight() {
  local expected_self_sha=$1
  local expected_update_sha=$2
  local expected_override_sha=$3
  shift 3
  env \
    PATH="${FAKE_BIN}:${PATH}" \
    MAPLELAND_PREFLIGHT_TEST_MODE=1 \
    MAPLELAND_PREFLIGHT_TEST_ROOT="${ROOT_DIR}" \
    "$@" \
    bash "${PREFLIGHT_SCRIPT}" \
      "${expected_self_sha}" \
      "${expected_update_sha}" \
      "${expected_override_sha}"
}

SELF_SHA=$(shasum -a 256 "${PREFLIGHT_SCRIPT}" | awk '{print $1}')
readonly SELF_SHA
UPDATE_SHA=$(shasum -a 256 "${UPDATE_SCRIPT}" | awk '{print $1}')
readonly UPDATE_SHA
OVERRIDE_SHA=$(shasum -a 256 "${COMPOSE_OVERRIDE_SOURCE}" | awk '{print $1}')
readonly OVERRIDE_SHA

success_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" 2>&1) \
  || fail "valid host contract should pass: ${success_output}"
assert_contains "${success_output}" 'host_preflight=ok'
assert_contains "${success_output}" 'management_listener=absent'
assert_not_contains "${success_output}" 'fixture-ghcr-secret'
assert_not_contains "${success_output}" 'fixture-management-secret'

# Literal Compose interpolation expressions are intentional fixture data.
# shellcheck disable=SC2016
printf '%s\n' \
  '      - GRAFANA_CLOUD_URL=${GRAFANA_CLOUD_URL}' \
  '      - GRAFANA_CLOUD_USERNAME=${GRAFANA_CLOUD_USERNAME}' \
  '      - GRAFANA_CLOUD_PASSWORD=${GRAFANA_CLOUD_PASSWORD}' \
  >> "${ROOT_DIR}/opt/mapleland/docker-compose.yml"
printf '%s\n' \
  'GRAFANA_CLOUD_URL=https://example.invalid' \
  'GRAFANA_CLOUD_USERNAME=fixture-user' \
  'GRAFANA_CLOUD_PASSWORD=fixture-legacy-secret' \
  >> "${ROOT_DIR}/opt/mapleland/.env"
transition_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" 2>&1) \
  || fail "complete first-rollout rollback contract should pass: ${transition_output}"
assert_contains "${transition_output}" 'legacy_rollback_contract=preserved'
assert_not_contains "${transition_output}" 'fixture-legacy-secret'
for transition_file in \
  "${ROOT_DIR}/opt/mapleland/docker-compose.yml" \
  "${ROOT_DIR}/opt/mapleland/.env"; do
  awk '!/GRAFANA_CLOUD_(URL|USERNAME|PASSWORD)/' \
    "${transition_file}" > "${transition_file}.clean"
  mv "${transition_file}.clean" "${transition_file}"
done

if mismatch_output=$(run_preflight "${SELF_SHA}" \
  '0000000000000000000000000000000000000000000000000000000000000000' \
  "${OVERRIDE_SHA}" \
  2>&1); then
  fail 'stale update-api.sh checksum should fail'
fi
assert_contains "${mismatch_output}" 'update-api.sh checksum mismatch'

if override_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" \
  '0000000000000000000000000000000000000000000000000000000000000000' \
  2>&1); then
  fail 'stale Compose observability override checksum should fail'
fi
assert_contains "${override_output}" 'docker-compose.observability.yml checksum mismatch'

if mode_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" \
  FAKE_GHCR_MODE=644 2>&1); then
  fail 'broad ghcr.env permissions should fail'
fi
assert_contains "${mode_output}" 'ghcr.env must be owned by root with mode 0600'

if listener_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" \
  FAKE_MANAGEMENT_LISTENER=wildcard 2>&1); then
  fail 'wildcard management listener should fail'
fi
assert_contains "${listener_output}" 'management port must be absent or bound only to 127.0.0.1'

if compose_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" \
  FAKE_COMPOSE_VALID=0 2>&1); then
  fail 'Compose without the observability boundary should fail'
fi
assert_contains "${compose_output}" 'Compose observability contract is incomplete'

if legacy_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" \
  FAKE_LEGACY_GRAFANA_ENV=1 2>&1); then
  fail 'legacy appender credentials in the app environment should fail'
fi
assert_contains "${legacy_output}" 'Compose observability contract is incomplete'
assert_not_contains "${legacy_output}" 'fixture-legacy-secret'

awk '
  /^RECOMMENDATION_V1_ENGINE=/ { print "RECOMMENDATION_V1_ENGINE=INVALID"; next }
  { print }
' "${ROOT_DIR}/opt/mapleland/.env" > "${ROOT_DIR}/opt/mapleland/.env.invalid"
mv "${ROOT_DIR}/opt/mapleland/.env.invalid" "${ROOT_DIR}/opt/mapleland/.env"
if engine_output=$(run_preflight "${SELF_SHA}" "${UPDATE_SHA}" "${OVERRIDE_SHA}" 2>&1); then
  fail 'invalid recommendation engine should fail'
fi
assert_contains "${engine_output}" 'RECOMMENDATION_V1_ENGINE must be AURA or MYSQL'

echo 'host-preflight tests passed'
