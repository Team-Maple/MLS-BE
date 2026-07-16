package com.maple.api.map.recommendation.adapter;

import com.maple.api.map.recommendation.domain.RecommendationCandidate;
import com.maple.api.map.recommendation.domain.RecommendationReason;
import com.maple.api.map.recommendation.domain.RecommendationScoringService;
import com.maple.api.map.recommendation.config.RecommendationDomainConfig;
import com.maple.api.map.recommendation.config.RecommendationProperties;
import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySqlRecommendationEvidenceAdapterIntegrationTest {

    private static final int PERFORMANCE_EVIDENCE_COUNT = 3_000;
    private static final int PERFORMANCE_CANDIDATE_COUNT = 100;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("mapledb")
            .withUsername("maple")
            .withPassword("maple");

    private static DataSource rawDataSource;
    private static JdbcTemplate rawJdbc;
    private static MySqlRecommendationEvidenceAdapter adapter;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        DriverManagerDataSource mysqlDataSource = new DriverManagerDataSource();
        mysqlDataSource.setDriverClassName(MYSQL.getDriverClassName());
        mysqlDataSource.setUrl(MYSQL.getJdbcUrl());
        mysqlDataSource.setUsername(MYSQL.getUsername());
        mysqlDataSource.setPassword(MYSQL.getPassword());
        rawDataSource = mysqlDataSource;
        rawJdbc = new JdbcTemplate(rawDataSource);

        new ResourceDatabasePopulator(
                new ClassPathResource("sql/recommendation-mysql8-schema.sql")
        ).execute(rawDataSource);
        seedContractFixture();
        seedPerformanceFixture();

        DataSource countedDataSource = ProxyDataSourceBuilder
                .create("recommendation-mysql8", rawDataSource)
                .countQuery()
                .build();
        adapter = new MySqlRecommendationEvidenceAdapter(
                new NamedParameterJdbcTemplate(countedDataSource),
                new RecommendationScoringService()
        );
    }

    @Test
    @Order(1)
    void executesMysql8CteAndWindowWithOneRoundTripAndRecordsRepresentativeLatency() {
        assertThat(rawJdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM recommendation_reviewed_claims
                 WHERE reviewed_claim_id >= 10000
                """, Integer.class)).isEqualTo(PERFORMANCE_EVIDENCE_COUNT);
        assertThat(rawJdbc.queryForObject("""
                SELECT COUNT(DISTINCT final_map_id)
                  FROM recommendation_reviewed_claims
                 WHERE reviewed_claim_id >= 10000
                """, Integer.class)).isEqualTo(PERFORMANCE_CANDIDATE_COUNT);

        QueryCountHolder.clear();
        long coldStartedAt = System.nanoTime();
        List<RecommendationCandidate> coldResult = adapter.recommend(50, 911, 20);
        double coldMillis = elapsedMillis(coldStartedAt);

        assertThat(coldResult).hasSize(20);
        assertThat(QueryCountHolder.getGrandTotal().getTotal()).isEqualTo(1);

        List<Double> warmMillis = new ArrayList<>();
        for (int iteration = 0; iteration < 40; iteration++) {
            QueryCountHolder.clear();
            long startedAt = System.nanoTime();
            List<RecommendationCandidate> result = adapter.recommend(50, 911, 20);
            warmMillis.add(elapsedMillis(startedAt));
            assertThat(result).hasSize(20);
            assertThat(QueryCountHolder.getGrandTotal().getTotal()).isEqualTo(1);
        }

        String executableQuery = MySqlRecommendationEvidenceAdapter.SCORING_QUERY
                .replace(":jobId", "911");
        String jsonPlan = rawJdbc.queryForObject("EXPLAIN FORMAT=JSON " + executableQuery, String.class);
        assertThat(jsonPlan)
                .contains("recommendation_reviewed_claims")
                .contains("alrim")
                .contains("idx_recommendation_reviewed_claims_scoring")
                .contains("idx_alrim_type_date");

        String planSummary = rawJdbc.queryForList("EXPLAIN " + executableQuery).stream()
                .map(row -> "%s:%s:%s".formatted(row.get("table"), row.get("type"), row.get("key")))
                .collect(Collectors.joining(","));

        warmMillis.sort(Comparator.naturalOrder());
        System.out.printf(
                "RECOMMENDATION_PERF mysql=%s evidence_rows=%d candidate_maps=%d final_results=%d "
                        + "queries_per_request=1 cold_ms=%.3f warm_p50_ms=%.3f warm_p95_ms=%.3f "
                        + "warm_p99_ms=%.3f plan=%s%n",
                MYSQL.getContainerInfo().getConfig().getImage(),
                PERFORMANCE_EVIDENCE_COUNT,
                PERFORMANCE_CANDIDATE_COUNT,
                coldResult.size(),
                coldMillis,
                percentile(warmMillis, 50),
                percentile(warmMillis, 95),
                percentile(warmMillis, 99),
                planSummary
        );
    }

    @Test
    @Order(2)
    void enforcesApprovalLineageDedupMapIdentityPolarityAndFacetContracts() {
        QueryCountHolder.clear();

        List<RecommendationCandidate> result = adapter.recommend(50, 111, 20);

        assertThat(QueryCountHolder.getGrandTotal().getTotal()).isEqualTo(1);
        Map<Long, RecommendationCandidate> byMap = result.stream()
                .collect(Collectors.toMap(RecommendationCandidate::mapId, Function.identity()));

        assertThat(byMap.get(1000L).score()).isEqualTo(0.95d);
        assertThat(byMap.get(1000L).reasons()).containsExactly(
                new RecommendationReason("reward", "xp"),
                new RecommendationReason("play_style", "solo"),
                new RecommendationReason("operability", "budget")
        );

        assertThat(byMap.get(1004L).score()).isEqualTo(1.0d);
        assertThat(byMap.get(1004L).reasons())
                .containsExactly(new RecommendationReason("reward", "xp"));
        assertThat(byMap).containsKeys(1005L, 1006L, 1007L, 1008L);
        assertThat(byMap).doesNotContainKeys(
                1001L,
                1002L,
                1003L,
                1009L,
                1010L,
                1011L,
                1999L
        );

        assertThat(adapter.loadEvidence(111))
                .filteredOn(row -> row.extractedClaimId() == 7L && row.mapId() == 1004L)
                .singleElement()
                .satisfies(row -> assertThat(row.jobId()).isEqualTo(111L));
    }

    @Test
    @Order(3)
    void configuredJdbcStatementTimeoutCancelsSlowMysqlQuery() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setQueryTimeoutSeconds(1);
        NamedParameterJdbcTemplate timeoutTemplate = new RecommendationDomainConfig()
                .recommendationJdbcTemplate(rawDataSource, properties);

        assertThatThrownBy(() -> timeoutTemplate.queryForObject(
                "SELECT SLEEP(3)",
                Map.of(),
                Integer.class
        )).isInstanceOf(DataAccessException.class);
    }

    private static void seedContractFixture() {
        rawJdbc.update("""
                INSERT INTO jobs(job_id, parent_job_id) VALUES
                    (100, NULL),
                    (110, 100),
                    (111, 110),
                    (112, 111),
                    (200, NULL),
                    (900, NULL),
                    (910, 900),
                    (911, 910)
                """);
        rawJdbc.update("""
                INSERT INTO maps(map_id) VALUES
                    (1000), (1001), (1002), (1003), (1004), (1005),
                    (1006), (1007), (1008), (1009), (1010), (1011)
                """);
        rawJdbc.update("""
                INSERT INTO alrim(type, date) VALUES
                    ('PATCH_NOTE', '2026-01-10 00:00:00'),
                    ('PATCH_NOTE', '2026-02-10 00:00:00'),
                    ('NOTICE', '2026-03-10 00:00:00')
                """);

        claim(1, "2026-03-01 00:00:00");
        reviewed(1, 1, "APPROVED", 1000, 111, 40, 60, "POSITIVE");
        reason(1, "reward", "xp", 0);
        reason(1, "play_style", "solo", 1);

        claim(2, "2026-01-01 00:00:00");
        reviewed(2, 2, "APPROVED", 1000, 110, 40, 60, "POSITIVE");
        reason(2, "reward", "xp", 0);
        reason(2, "play_style", "party", 1);
        reason(2, "operability", "budget", 2);

        claim(3, "2026-02-01 00:00:00");
        reviewed(3, 3, "APPROVED", 1000, 100, 40, 60, "NEGATIVE");
        reason(3, "reward", "meso", 0);
        reason(3, "operability", "fatigue", 1);

        claim(4, "2026-03-01 00:00:00");
        reviewed(4, 4, "PENDING", 1001, 111, 40, 60, "POSITIVE");

        claim(5, "2026-03-01 00:00:00");
        reviewed(5, 5, "APPROVED", 1002, 112, 40, 60, "POSITIVE");

        claim(6, "2026-03-01 00:00:00");
        reviewed(6, 6, "APPROVED", 1003, 200, 40, 60, "POSITIVE");

        claim(7, "2026-03-01 00:00:00");
        reviewed(7, 7, "APPROVED", 1004, 111, 40, 60, "POSITIVE");
        reviewed(8, 7, "APPROVED", 1004, 110, 40, 60, "POSITIVE");
        reason(7, "reward", "xp", 0);
        reason(8, "reward", "meso", 0);

        claim(8, "2026-03-01 00:00:00");
        reviewed(9, 8, "APPROVED", 1005, 111, 40, 60, "POSITIVE");
        reviewed(10, 8, "APPROVED", 1006, 110, 40, 60, "POSITIVE");

        claim(9, "2026-03-01 00:00:00");
        reviewed(11, 9, "APPROVED", 1007, 111, 50, null, "POSITIVE");

        claim(10, "2026-03-01 00:00:00");
        reviewed(12, 10, "APPROVED", 1008, 111, null, 50, "POSITIVE");

        claim(11, "2026-03-01 00:00:00");
        reviewed(13, 11, "APPROVED", 1011, 111, null, null, "POSITIVE");

        claim(12, "2026-03-01 00:00:00");
        reviewed(14, 12, "APPROVED", 1009, 111, 40, 60, "NEGATIVE");

        claim(13, "2026-03-01 00:00:00");
        reviewed(15, 13, "APPROVED", 1010, 111, 40, 60, "POSITIVE");
        claim(14, "2026-03-01 00:00:00");
        reviewed(16, 14, "APPROVED", 1010, 111, 40, 60, "NEGATIVE");

        claim(15, "2026-03-01 00:00:00");
        reviewed(17, 15, "APPROVED", 1999, 111, 40, 60, "POSITIVE");
    }

    private static void seedPerformanceFixture() throws Exception {
        try (Connection connection = rawDataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement map = connection.prepareStatement("INSERT INTO maps(map_id) VALUES (?)");
                 PreparedStatement extracted = connection.prepareStatement("""
                         INSERT INTO recommendation_extracted_claims(
                             extracted_claim_id, source_published_at
                         ) VALUES (?, ?)
                         """);
                 PreparedStatement reviewed = connection.prepareStatement("""
                         INSERT INTO recommendation_reviewed_claims(
                             reviewed_claim_id, extracted_claim_id, review_status,
                             final_map_id, final_job_id, final_level_min, final_level_max, final_polarity
                         ) VALUES (?, ?, 'APPROVED', ?, ?, 40, 60, ?)
                         """);
                 PreparedStatement reason = connection.prepareStatement("""
                         INSERT INTO recommendation_reviewed_claim_reasons(
                             reviewed_claim_id, reason_axis, reason_value, display_order
                         ) VALUES (?, 'reward', 'xp', 0)
                         """)) {
                for (int mapOffset = 0; mapOffset < PERFORMANCE_CANDIDATE_COUNT; mapOffset++) {
                    map.setLong(1, 2000L + mapOffset);
                    map.addBatch();
                }
                map.executeBatch();

                Timestamp publishedAt = Timestamp.valueOf("2026-01-01 00:00:00");
                for (int index = 0; index < PERFORMANCE_EVIDENCE_COUNT; index++) {
                    long id = 10_000L + index;
                    extracted.setLong(1, id);
                    extracted.setTimestamp(2, publishedAt);
                    extracted.addBatch();

                    reviewed.setLong(1, id);
                    reviewed.setLong(2, id);
                    reviewed.setLong(3, 2000L + index % PERFORMANCE_CANDIDATE_COUNT);
                    reviewed.setLong(4, 900L + switch (index % 3) {
                        case 0 -> 0L;
                        case 1 -> 10L;
                        default -> 11L;
                    });
                    reviewed.setString(5, (index / PERFORMANCE_CANDIDATE_COUNT) % 4 == 0
                            ? "NEGATIVE"
                            : "POSITIVE");
                    reviewed.addBatch();

                    reason.setLong(1, id);
                    reason.addBatch();

                    if ((index + 1) % 500 == 0) {
                        extracted.executeBatch();
                        reviewed.executeBatch();
                        reason.executeBatch();
                    }
                }
                connection.commit();
            }
        }
    }

    private static void claim(long extractedClaimId, String publishedAt) {
        rawJdbc.update(
                "INSERT INTO recommendation_extracted_claims(extracted_claim_id, source_published_at) VALUES (?, ?)",
                extractedClaimId,
                Timestamp.valueOf(publishedAt)
        );
    }

    private static void reviewed(
            long reviewedClaimId,
            long extractedClaimId,
            String status,
            long mapId,
            long jobId,
            Integer levelMin,
            Integer levelMax,
            String polarity
    ) {
        rawJdbc.update("""
                        INSERT INTO recommendation_reviewed_claims(
                            reviewed_claim_id, extracted_claim_id, review_status,
                            final_map_id, final_job_id, final_level_min, final_level_max, final_polarity
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                reviewedClaimId,
                extractedClaimId,
                status,
                mapId,
                jobId,
                levelMin,
                levelMax,
                polarity
        );
    }

    private static void reason(long reviewedClaimId, String axis, String value, int displayOrder) {
        rawJdbc.update("""
                        INSERT INTO recommendation_reviewed_claim_reasons(
                            reviewed_claim_id, reason_axis, reason_value, display_order
                        ) VALUES (?, ?, ?, ?)
                        """,
                reviewedClaimId,
                axis,
                value,
                displayOrder
        );
    }

    private static double elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000.0d;
    }

    private static double percentile(List<Double> sortedValues, int percentile) {
        int index = Math.max(0, (int) Math.ceil(percentile / 100.0d * sortedValues.size()) - 1);
        return sortedValues.get(index);
    }
}
