# MySQL 근거 기반 사냥터 추천 롤아웃 가이드

이 문서는 Issue [#34](https://github.com/Team-Maple/MLS-BE/issues/34)의 구현을 검증하고 단계적으로 운영에 적용하기 위한 개발자·운영자용 체크리스트다. 공개 API 계약과 운영 DB read path를 함께 변경하는 고위험 작업이므로 코드 리뷰와 위험 리뷰를 모두 통과해야 한다. 이 문서 작성 시점에는 구현 branch를 merge하거나 운영에 배포하지 않았으며, 아래 단계는 owner의 명시적 승인 없이는 실행하지 않는다.

## 관련 작업과 범위

- MLS-BE 구현 Issue: [Team-Maple/MLS-BE#34](https://github.com/Team-Maple/MLS-BE/issues/34)
- upstream comparator 오류: [mungmnb777/mapleland#130](https://github.com/mungmnb777/mapleland/issues/130)
- mapledb scoring index: [mungmnb777/mapleland#131](https://github.com/mungmnb777/mapleland/issues/131), [draft PR #132](https://github.com/mungmnb777/mapleland/pull/132)
- 최초 운영 기본값: `RECOMMENDATION_V1_ENGINE=AURA`
- v2 엔진: MySQL 고정, 최초 운영 노출은 `RECOMMENDATION_V2_ENABLED=false`
- scorer/transaction timeout: `RECOMMENDATION_QUERY_TIMEOUT_SECONDS=10` (허용 1~60초)
- 기본 `limit`: 5, 허용 범위: 1~20, 최종 정렬 뒤 적용

MLS-BE는 mapleland의 HTTP test endpoint를 호출하지 않는다. 같은 MySQL의 승인된 추천 테이블을 query-only로 직접 읽고 MLS-BE 안에서 점수를 계산한다. importer, crawler, admin UI/API, alias writer, resolve-pending, FAMILY 관리 기능은 범위 밖이다. FAMILY alias fan-out은 reviewed claim에 이미 materialize되어 있으므로 요청 시 alias를 다시 해소하지 않는다. AuraDB dependency, 설정, keep-alive, secret 제거도 안정화 이후 별도 Issue/PR에서 처리한다.

## upstream 근거와 adaptation

승인된 기준 구현은 mapleland commit `bfa2af8e9135b53b51a0891a9d5187a21b74d2af`다. 조사한 upstream 작업 checkout의 HEAD는 `79f7a036d879e7ddd6254dcedab86d89f73a438e`로 기준보다 6 commits 앞서 있었지만, 이번 작업에서 참조한 추천 service/port/adapter/domain/test, ADR, admin schema에는 두 commit 사이의 차이가 없었다. 별도 schema index 작업은 최신 `main` `353dc4a98ecaff4e9a1e211369b744518f92cac6`를 기준으로 추적한다.

MLS-BE adaptation은 다음 차이를 의도적으로 둔다.

- Job lineage를 MySQL 8 recursive CTE 한 번으로 읽는다.
- claim별 patch count, 조상별 claim, claim별 reason 쿼리를 하나의 scoring 쿼리로 합친다.
- map과 로그인 사용자의 bookmark는 각각 bulk 조회한다.
- upstream의 고정 `.limit(3)`을 가져오지 않고 API `limit` 계약을 적용한다.
- upstream의 chained `.reversed()` comparator를 가져오지 않고 각 정렬 키의 방향을 명시한다.
- v1과 v2 DTO를 분리해 v1 strict decoder 계약을 보존한다.
- 요청 단위 Aura/MySQL dual-read와 MySQL 오류 시 자동 Aura fallback을 하지 않는다.

## 점수 계약

각 deduplicated evidence `i`의 기여도는 다음과 같다.

```text
levelMatch_i = requestedLevel이 유효 레벨 범위 안이면 1, 아니면 0
freshnessWeight_i = round(max(0.1, 1.0 - 0.05 * 이후 PATCH_NOTE 수), 3)
polaritySign_i = positive이면 +1, negative이면 -1
contribution_i = polaritySign_i * freshnessWeight_i * levelMatch_i
mapScore = 같은 mapId에 속한 contribution_i의 합
```

유효 레벨 범위는 양쪽 bound가 있으면 inclusive `[levelMin, levelMax]`다. `levelMin`만 있으면 `levelMax = levelMin + 10`, `levelMax`만 있으면 `levelMin = levelMax - 10`으로 본다. 양쪽이 모두 없으면 점수화하지 않는다.

다음 예시는 domain test와 MySQL 8 integration fixture 양쪽에 고정한다.

```text
positive, 이후 patch 0회: +1.00
positive, 이후 patch 2회: +0.90
negative, 이후 patch 1회: -0.95
최종 mapScore: 0.95
```

점수에 `confidence_score`, 조회 수, 좋아요/싫어요, 댓글 수, 요청 Job과 조상 Job 간 별도 가중치, AuraDB의 `levelHits`/`jobHits`, 임의 정규화를 추가하지 않는다. 반환하는 `score`는 선택 엔진의 실제 점수이며, MySQL 선택 시 위 evidence net score다.

### 조회와 후보 불변식

- `review_status = 'APPROVED'`인 근거만 사용한다.
- 요청 Job 자신과 조상 Job의 근거만 사용하고 자손·무관 Job을 제외한다.
- 정확한 요청 Job과 조상 Job의 contribution 가중치는 같다.
- 같은 `extracted_claim_id + final_map_id`가 lineage 여러 단계에 있으면 한 번만 합산한다.
- 같은 extracted claim이라도 서로 다른 `final_map_id`는 별도 후보로 유지한다.
- positive와 negative를 모두 합산한다.
- 레벨이 일치하는 positive 근거가 하나 이상이고 최종 `mapScore > 0`인 맵만 반환한다.
- 최종 점수는 유한한 `double`이어야 하며 NaN/Infinity이면 실패 처리한다.

최종 정렬은 다음 키를 순서대로 적용한다.

1. `mapScore` 내림차순
2. `freshnessSum` 내림차순. 여기서 `freshnessSum`은 합산된 contribution 절댓값의 합이다.
3. 가장 높은 contribution을 만든 근거의 `publishedAt` 내림차순
4. `mapId` 오름차순

동일한 최고 contribution을 만든 근거가 여러 개면 더 최신 `publishedAt`을 대표 근거로 선택한다. 높은 `mapScore`가 먼저 오는 regression test로 upstream Issue #130의 comparator 오류가 재발하지 않게 한다.

## 추천 이유 계약

reason facet도 해당 evidence의 signed contribution을 같은 방식으로 합산한다. 축별 누적 weight가 0보다 큰 facet 중 가장 큰 하나만 반환하고, 동률이면 아래 선언 순서의 priority를 사용한다.

| axis | 허용 value와 priority |
| --- | --- |
| `reward` | `xp`, `meso`, `loot` |
| `play_style` | `solo`, `party`, `party_quest` |
| `operability` | `fatigue`, `mobility`, `budget` |

응답 순서는 `reward`, `play_style`, `operability`다. reason text, excerpt, claim 원문, 작성자 정보는 SQL select와 공개 DTO에 포함하지 않는다.

## 공개 API 계약

### v1

`GET /api/v1/maps/recommendations?level={1..200}&jobId={id}&limit={1..20}`

outer wrapper, validation, status/error 계약, 익명·로그인 접근 정책을 유지한다. item의 JSON key는 아래 다섯 개뿐이며 `reasons`, `facets`, `hasRecommendation` 같은 additive field도 허용하지 않는다. 추천이 없으면 `data: []`다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": null,
  "data": [
    {
      "mapId": 100000000,
      "score": 0.95,
      "iconUrl": "https://...",
      "nameKr": "헤네시스",
      "bookmarkId": null
    }
  ]
}
```

`RECOMMENDATION_V1_ENGINE=AURA|MYSQL`로 선택한다. 최초 production 배포에서는 `AURA`를 유지한다. owner가 DB preflight와 v2 결과를 승인해 `MYSQL`로 바꾼 뒤에는 `score` 의미가 Aura hit score에서 evidence net score로 바뀌지만 JSON shape은 그대로다.

### v2

`GET /api/v2/maps/recommendations?level={1..200}&jobId={id}&limit={1..20}`

parameter, limit, 선택적 인증, bookmark, outer wrapper는 v1과 같다. 별도 v2 DTO가 `reasons`만 추가하며 이 값은 항상 배열이고 이유가 없으면 `[]`다.

v2는 MySQL 고정이므로 별도 kill switch인 `RECOMMENDATION_V2_ENABLED`를 둔다. production 기본값은 `false`이며 topology/schema/실행 계획과 외부 rate-limit gate를 확인한 뒤 owner 승인으로만 `true`로 전환한다. 비활성 상태는 DB를 읽지 않고 기존 recommendation unavailable 503을 반환한다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": null,
  "data": [
    {
      "mapId": 100000000,
      "score": 0.95,
      "iconUrl": "https://...",
      "nameKr": "헤네시스",
      "bookmarkId": null,
      "reasons": [
        {
          "axis": "reward",
          "value": "xp"
        }
      ]
    }
  ]
}
```

존재하지 않는 Job은 기존 not-found 계약을 사용한다. 선택 엔진이나 추천 schema를 사용할 수 없을 때 애플리케이션 시작 자체를 실패시키지 않고 해당 endpoint에서 기존 `MAP_RECOMMENDATION_UNAVAILABLE` 계열 503을 반환한다.

## DB read-only preflight 증거

2026-07-16에 접근 가능한 mapleland DB를 read-only transaction으로 확인한 결과다. credential, endpoint, 원본 환경 파일 값은 기록하지 않았다.

| 항목 | 확인 결과 |
| --- | --- |
| MySQL | 8.0.42 Community |
| schema | `mapledb` |
| 추천/Job/Map table과 필수 column | 존재 |
| `APPROVED` reviewed claim | 3,010 rows |
| 전체 reason | 11,955 rows |
| `APPROVED` claim에 연결된 reason | 4,817 rows |
| `PATCH_NOTE` | 95 rows |
| canonical map orphan | 0 |
| canonical job orphan | 0 |
| Job parent cycle | 0 |
| 관찰된 최대 Job lineage depth | 3 |
| 중복 `extracted_claim_id + final_map_id` group/surplus | 0 / 0 |
| 양쪽 level bound가 모두 null인 `APPROVED` claim | 0 |

현재 index가 없는 상태의 `EXPLAIN`에서는 `recommendation_reviewed_claims`가 `ALL`, key 없음, 약 7,649 rows였고 `alrim`이 `ALL`, key 없음, 약 464 rows였다. 이 결과가 schema Issue #131의 근거다.

### 운영 topology blocker

mapleland가 접속한 `mapledb`에 MLS canonical table과 추천 table이 함께 있다는 사실은 확인했다. 그러나 현재 접근 권한으로 MLS-BE production의 `${DB_URL}`이 같은 MySQL endpoint와 schema를 가리키는지는 독립적으로 입증하지 못했다. 로컬 runtime credential이 없었고 manual OCI SSH identity도 사용할 수 없었다.

따라서 다음을 secret-safe/read-only 방식으로 확인하기 전에는 v2 운영 검증과 v1 MySQL 전환을 진행하지 않는다.

- MLS-BE와 mapleland의 DB host/port/schema가 동일한지 값을 출력하지 않고 equality만 확인
- 운영 연결 사용자에게 필수 table/column과 필요한 read 권한이 있는지 확인
- 위 row count, orphan, cycle, duplicate 검사를 운영 endpoint에서 재실행
- 실제 scoring query의 `EXPLAIN FORMAT=JSON`과 선택 index 확인
- `APPROVED` polarity 및 reason axis/value allowlist 위반이 0인지 확인

topology가 다르면 credential, cross-schema 권한, 복제 table을 임의로 만들지 않고 blocker로 owner에게 보고한다.

## index와 schema ownership

필요한 index는 mapledb owner인 mapleland의 Issue #131에서 forward/rollback/preflight SQL로 관리한다.

```sql
CREATE INDEX idx_recommendation_reviewed_claims_scoring
    ON recommendation_reviewed_claims (review_status, final_job_id);

CREATE INDEX idx_alrim_type_date
    ON alrim (type, date);
```

Testcontainers fixture에는 두 index를 넣어 실행 계획에서 사용 가능성을 검증했다. `EXPLAIN FORMAT=JSON`은 reviewed-claim index를 possible key로 인식했고 patch index는 실제 선택했다. 다만 synthetic fixture가 100% `APPROVED`이고 모든 row가 요청 lineage에 일치하도록 구성돼 reviewed claim은 full scan이 더 저렴하다고 판단됐다. 이 결과를 production index 선택 증거로 확대 해석하지 않는다. 실제 production DDL은 실행하지 않았고 Hibernate `ddl-auto`에도 맡기지 않는다. 별도 schema PR 검토, owner 승인, production preflight와 change window를 거친 뒤 forward SQL을 실행한다. rollback SQL은 index 이름·존재 여부를 preflight한 뒤에만 사용한다.

schema PR preflight는 reason table/column/index, polarity·facet allowlist, MLS-BE의 consolidated lineage/dedup/patch/reason query 전체 `EXPLAIN FORMAT=TREE`를 검사하도록 보강했다. 이 EXPLAIN이 성공하면 해당 session의 필수 table SELECT 권한도 함께 확인된다. 다만 MLS-BE production credential로 같은 결과를 얻는 gate는 topology blocker가 해소될 때까지 남아 있다.

## query budget과 성능 증거

한 요청의 고정 query path는 다음과 같다.

| 단계 | 최대 round trip | 조건 |
| --- | ---: | --- |
| Job 존재 확인 | 1 | 모든 정상 validation 요청 |
| lineage/evidence/patch/reason scoring | 1 | MySQL 엔진 선택 시 |
| canonical map bulk 조회 | 1 | 후보가 있을 때 |
| bookmark bulk 조회 | 1 | 후보가 있고 로그인했을 때 |

따라서 MySQL 추천은 후보·evidence 수와 무관하게 로그인 결과 요청 최대 4회, 익명 결과 요청 최대 3회다. 빈 후보는 enrichment 쿼리를 생략한다. adapter integration query counter는 scorer가 요청당 정확히 1회임을 cold 1회와 warm 40회 모두 확인했고, enrichment unit test는 map과 bookmark bulk port가 각각 한 번만 호출되고 scorer 정렬 순서를 보존함을 확인했다. 단일 proxy가 controller 전체 DB 호출을 합산하는 end-to-end 계측은 아직 별도 증거가 없으므로 위 전체 횟수는 각 검증된 경계를 합친 query budget이다.

MySQL scoring JDBC statement와 이를 감싸는 read-only transaction에는 같은 configurable 10초 timeout을 적용한다. Testcontainers의 `SELECT SLEEP(3)`를 1초 statement timeout으로 취소하는 계약 테스트를 포함한다. 기존 Aura v1의 relational transaction에는 이 새 timeout을 적용하지 않아 slow Aura 호출 뒤 enrichment가 임의로 503이 되는 호환 회귀를 막는다. v2 default-off kill switch는 긴급 MySQL traffic drain에 사용한다. reverse proxy의 recommendation 전용 rate-limit 설정은 현재 저장소/접근 범위에서 입증하지 못했으므로 이를 확인하기 전에는 v2를 운영 공개하지 않는다. 측정 없이 cache를 추가하지 않았다.

로컬 Testcontainers `mysql:8.4`의 대표 fixture 측정값은 다음과 같다.

| evidence rows | candidate maps | final results | scorer queries/request | cold | warm p50 | warm p95 | warm p99 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3,000 | 100 | 20 | 1 | 58.938 ms | 25.266 ms | 38.832 ms | 45.933 ms |

이 수치는 개발 machine의 단일 container 측정이며 production SLO나 alert threshold가 아니다. MySQL 8 recursive CTE/window 실행, 결과 수와 고정 query count를 검증한 characterization evidence다. 같은 fixture의 tabular plan summary는 다음과 같았다.

```text
<derived3>:ALL:null, reason:ref:uk_recommendation_reviewed_claim_reasons,
<derived8>:ref:<auto_key0>, <derived3>:ALL:null,
patch:ref:idx_alrim_type_date, rc:ALL:null, ec:eq_ref:PRIMARY,
canonical_map:eq_ref:PRIMARY, <derived4>:ref:<auto_key0>,
j:const:PRIMARY, lineage:ALL:null, parent:eq_ref:PRIMARY
```

`idx_alrim_type_date`는 선택됐지만 `idx_recommendation_reviewed_claims_scoring`은 JSON의 possible key로만 나타났고, 위 fixture 특성 때문에 optimizer가 `rc:ALL`을 선택했다. production에서 DDL을 승인·적용한 뒤 실제 분포로 다시 실행한 `EXPLAIN`은 아직 없다. 운영 latency와 Aura baseline 비교는 production topology 확인과 안전한 v2 smoke 뒤 기존 `http.server.requests` 표본으로 수행한다. 표본이 부족하면 부족하다고 기록하며 운영 traffic을 부하 테스트에 사용하지 않는다.

## 관측성과 민감정보 경계

기존 `http.server.requests`의 route-template metric으로 v1/v2 request rate, latency, HTTP error를 본다. 같은 latency를 재는 custom timer는 추가하지 않는다. custom metric은 추천 outcome/engine/api version과 result count만 기록한다.

- request counter label: `engine`, `api_version`, `outcome`
- result summary label: `engine`, `api_version`
- 금지 label: jobId, level, mapId, member ID, raw URI/query
- 허용 structured log field: `event.action`, `event.outcome`, recommendation engine, API version, duration, result count
- 금지 log: member ID, 요청 level/job, query string, claim/reason 원문, credential
- 예외는 기존 `SafeExceptionLog` 정책을 사용한다.

versioned dashboard JSON과 live Grafana dashboard UID `mapleland-production-overview`는 recommendation row의 실제 v1/v2 HTTP request rate, 기존 HTTP p95, HTTP error와 custom empty/unavailable outcome, 관찰된 engine, 평균 result count 쿼리를 포함하도록 함께 갱신했다. total request/error에는 `http.server.requests` route/status를 사용하므로 config parsing, validation, 404도 빠지지 않는다. custom counter는 scorer 처리 outcome 의미로만 사용한다. panel 24개를 유지하며 기존 UID와 `Dashboards` folder를 create-or-update했다. live version history에서 source와 같은 version `3`이 `2026-07-16 23:21:43 KST`의 Latest이고 version 2/1이 restore 가능한 상태임을 확인했다. 애플리케이션/Alloy 변경이 아직 배포되지 않았고 route traffic 표본도 없으므로 pre-deployment panel 렌더링은 live metric series 검증이 아니다. folder/datasource/contact point/notification policy/unrelated alert는 변경하지 않았고 baseline 없이 alert threshold를 추가하지 않았다.

## 배포 전 게이트

다음 항목이 모두 충족돼야 owner에게 배포 승인을 요청할 수 있다.

- [ ] Issue #34와 구현 draft PR에 v1/v2 계약, 점수 공식, DB 전제, rollout/rollback이 기록됨
- [ ] 독립 코드 리뷰와 위험 리뷰 finding을 한 라운드로 모아 반영하고 재검증함
- [x] 전체 Gradle test와 `bootJar`, 관련 배포/observability 계약 test가 성공함
- [ ] CI가 성공하고 unresolved required review가 없음
- [ ] mapleland schema PR의 forward/rollback/preflight SQL과 `EXPLAIN` 근거가 검토됨
- [ ] MLS-BE production DB topology blocker가 해소됨
- [ ] production reverse proxy의 recommendation route rate-limit이 확인되고 v2 공개량이 승인됨
- [ ] host `.env`, rendered Compose, candidate container에서 v1 engine/v2 enable/query timeout이 동일함을 값 노출 없이 검증함
- [ ] production DDL이 필요하면 owner가 별도로 승인함
- [x] Grafana versioned JSON과 live dashboard UID/version이 일치하고 새 panel query가 오류 없이 완료됨. 실제 series 검증은 배포 후 smoke gate로 남음
- [ ] merge와 운영 배포에 대한 owner의 명시적 승인이 있음

최소 로컬 검증 명령은 다음과 같다.

```bash
./gradlew clean test
./gradlew bootJar
jq empty deploy/observability/grafana/mapleland-production-overview.json
jq empty deploy/observability/grafana/alert-rules.json
bash deploy/observability/alloy/validate.sh
```

2026-07-16 최종 로컬 트리에서 `./gradlew clean test`는 88 tests, failure/error/skip 0으로 성공했고 `./gradlew bootJar`, 두 Grafana JSON `jq empty`, Alloy 공식 container validation, host preflight/update 계약 test, recommendation asset test, shell syntax·executable 검증도 성공했다. CI 결과와 run URL은 draft PR 생성 뒤 별도로 기록한다.

## 권장 rollout

1. 구현 PR을 merge하지 않은 상태에서 전체 test, 독립 코드 리뷰, 위험 리뷰, CI를 완료한다.
2. mapleland schema PR을 별도로 검토하고 topology equality와 운영 read 권한을 확인한다.
3. 필요 index의 production preflight와 `EXPLAIN`을 수행한다. DDL이 필요하면 owner 승인 뒤 forward SQL만 실행하고 결과를 기록한다.
4. host의 reviewed Compose override와 root-only `.env`에 `RECOMMENDATION_V1_ENGINE=AURA`, `RECOMMENDATION_V2_ENABLED=false`, `RECOMMENDATION_QUERY_TIMEOUT_SECONDS=10`을 명시하고 preflight가 rendered model을 검증하게 한다.
5. 구현 image를 위 설정으로 배포한다. candidate readiness는 실제 container env가 attested 설정과 같은지도 값 노출 없이 검증한다. 이 단계에서 v1은 기존 Aura 동작을 유지하고 v2는 DB를 읽지 않는 503 kill-switch 상태다.
6. topology/schema/full-plan, SELECT 권한, reverse-proxy rate-limit을 확인한 뒤 owner 승인으로 `RECOMMENDATION_V2_ENABLED=true`를 적용한다. v2 익명/로그인 smoke를 소량 수행해 exact contract, bookmark, score/reasons, empty, invalid input, missing Job, 503을 확인한다. 운영 부하 테스트는 하지 않는다.
7. 기존 dashboard UID를 create-or-update하고 v1/v2 rate, p95, error/empty, engine, result count를 관찰한다. 충분한 표본이 생길 때까지 임의 threshold를 만들지 않는다.
8. DB preflight, v2 결과, latency와 오류율을 owner가 승인한 뒤 별도 config change로 `RECOMMENDATION_V1_ENGINE=MYSQL`을 적용한다.
9. v1 exact five-key contract와 evidence score semantic change를 다시 smoke하고 bounded metric/log만 생성되는지 확인한다.
10. 안정화 기간 뒤 Aura dependency/config/keep-alive/secret 제거를 별도 Issue/PR로 진행한다.

## rollback

문제가 생기면 request 단위 fallback을 추가하지 않는다. 먼저 `RECOMMENDATION_V1_ENGINE=AURA`, `RECOMMENDATION_V2_ENABLED=false`로 attested config를 적용해 MySQL recommendation traffic을 drain한다. v2를 Aura로 의미 변경하지 않는다. 구현 자체의 회귀라면 승인된 이전 immutable image로 애플리케이션을 rollback한다.

index rollback은 애플리케이션 rollback과 분리한다. scoring traffic drain과 실행 계획을 확인하고 owner가 승인한 경우에만 schema Issue #131의 rollback SQL을 사용한다. topology 차이를 credential 추가, cross-schema grant, 임시 복제 table로 우회하지 않는다.

## 현재 미완료 또는 남은 위험

- MLS-BE production DB와 mapleland DB의 endpoint/schema equality가 확인되지 않았다.
- production reverse proxy의 recommendation 전용 rate-limit은 확인되지 않아 v2는 default-off다.
- production index DDL은 실행하지 않았다.
- live Grafana dashboard layout/version 갱신은 완료했지만 배포 전이므로 recommendation metric의 live series 검증은 남아 있다.
- 로컬 전체 test는 성공했지만 CI/required review의 최종 결과는 draft PR 생성 뒤 확인해야 한다.
- Testcontainers 수치는 local characterization이며 production latency 표본이 아니다.
- 이 문서 작성 시점에는 구현 merge, v1 MySQL 설정 전환, 운영 배포, Aura 제거를 수행하지 않았다.
