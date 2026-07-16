#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
config_file="${script_dir}/config.alloy"
alloy_image="grafana/alloy:v1.17.1"

# Placeholder values are deliberately non-secret. They allow Alloy's semantic
# validator to evaluate sys.env calls in CI and on developer workstations.
docker run --rm --read-only \
  --volume "${config_file}:/etc/alloy/config.alloy:ro" \
  "${alloy_image}" fmt --test /etc/alloy/config.alloy

docker run --rm --read-only \
  --env ALLOY_HOST_NAME=validate-host \
  --env MANAGEMENT_SCRAPE_TOKEN=validate \
  --env GRAFANA_CLOUD_METRICS_URL=https://example.invalid/api/prom/push \
  --env GRAFANA_CLOUD_METRICS_USERNAME=validate \
  --env GRAFANA_CLOUD_METRICS_API_TOKEN=validate \
  --env GRAFANA_CLOUD_LOGS_URL=https://example.invalid/loki/api/v1/push \
  --env GRAFANA_CLOUD_LOGS_USERNAME=validate \
  --env GRAFANA_CLOUD_LOGS_API_TOKEN=validate \
  --volume "${config_file}:/etc/alloy/config.alloy:ro" \
  "${alloy_image}" validate /etc/alloy/config.alloy

# Reject the common failure mode of committing a literal credential to the
# Alloy source. Runtime credentials must remain sys.env references.
if grep -E 'password[[:space:]]*=[[:space:]]*"[^"$]+"' "${config_file}"; then
  echo "literal password found in ${config_file}" >&2
  exit 1
fi
