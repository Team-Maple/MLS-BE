#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
observability_dir="$(cd -- "${script_dir}/.." && pwd)"
alloy_config="${observability_dir}/alloy/config.alloy"
dashboard="${observability_dir}/grafana/mapleland-production-overview.json"

fail() {
  printf '%s\n' "$1" >&2
  exit 1
}

jq empty "${dashboard}"

grep -Fq 'mapleland_recommendation_requests_total' "${alloy_config}" \
  || fail 'recommendation request counter is missing from the Alloy application allowlist'
grep -Fq 'mapleland_recommendation_results_recommendations_(count|sum|max)' "${alloy_config}" \
  || fail 'recommendation result summary is missing from the Alloy application allowlist'

for metadata_field in \
  event_duration \
  mapleland_recommendation_engine \
  mapleland_api_version \
  mapleland_result_count; do
  field_occurrences="$(grep -c "${metadata_field}" "${alloy_config}")"
  test "${field_occurrences}" -eq 2 \
    || fail "${metadata_field} must be extracted once and promoted to structured metadata once"
done

label_keep_block="$(sed -n '/stage.label_keep {/,/^\t}/p' "${alloy_config}")"
for forbidden_label in \
  event_duration \
  mapleland_recommendation_engine \
  mapleland_api_version \
  mapleland_result_count; do
  if grep -Fq "${forbidden_label}" <<<"${label_keep_block}"; then
    fail "${forbidden_label} must not become an indexed Loki label"
  fi
done

jq -e '
  .uid == "mapleland-production-overview"
  and ([.panels[].id] | index(18) < index(24))
  and ([.panels[] | select(.id == 18 and .type == "row" and .title == "Recommendations")] | length == 1)
  and ([.panels[] | select(.id == 24 and .type == "row" and .title == "Application logs")] | length == 1)
  and ([.panels[] | select(.id == 19 and .title == "Recommendation request rate")] | length == 1)
  and ([.panels[] | select(.id == 20 and .title == "Recommendation p95 latency")] | length == 1)
  and ([.panels[] | select(.id == 21 and .title == "Error / empty / unavailable rate")] | length == 1)
  and ([.panels[] | select(.id == 22 and .title == "Engine state (observed)")] | length == 1)
  and ([.panels[] | select(.id == 23 and .title == "Average result count")] | length == 1)
' "${dashboard}" >/dev/null || fail 'recommendation dashboard row or required panels are missing'

jq -e '
  ([.panels[] | select(.id == 19) | .targets[].expr | contains("http_server_requests_seconds_count")] | all)
  and ([.panels[] | select(.id == 19) | .targets[].expr | contains("/api/v[12]/maps/recommendations")] | all)
  and ([.panels[] | select(.id == 20) | .targets[].expr | contains("http_server_requests_seconds_bucket")] | all)
  and ([.panels[] | select(.id == 20) | .targets[].expr | contains("/api/v[12]/maps/recommendations")] | all)
  and ([.panels[] | select(.id == 21) | .targets[].expr | contains("empty|unavailable")] | any)
  and ([.panels[] | select(.id == 21) | .targets[].expr | contains("http_server_requests_seconds_count")] | any)
  and ([.panels[] | select(.id == 21) | .targets[].expr | contains("4..|5..") ] | any)
  and ([.panels[] | select(.id == 22) | .targets[].expr | contains("api_version, engine")] | all)
  and ([.panels[] | select(.id == 23) | .targets[].expr | contains("mapleland_recommendation_results_recommendations_sum")] | all)
  and ([.panels[] | select(.id == 23) | .targets[].expr | contains("mapleland_recommendation_results_recommendations_count")] | all)
' "${dashboard}" >/dev/null || fail 'recommendation dashboard queries do not match the metric contract'

recommendation_queries="$(jq -r '.panels[] | select(.id >= 19 and .id <= 23) | .targets[].expr' "${dashboard}")"
if grep -Eiq '(job_?id|level|map_?id|member_?id|user_?id|raw_?uri|query_string)' \
    <<<"${recommendation_queries}"; then
  fail 'recommendation dashboard query contains a prohibited high-cardinality or sensitive label'
fi

printf '%s\n' 'Recommendation observability asset contract passed.'
