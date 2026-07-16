package com.maple.api.observability;

import com.maple.api.map.recommendation.application.RecommendationObservability;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "management.server.port=0",
        "management.server.address=127.0.0.1",
        "management.endpoints.web.exposure.include=health,info,prometheus",
        "management.endpoint.health.show-details=never",
        "management.endpoint.health.show-components=never",
        "management.health.neo4j.enabled=false",
        "management.prometheus.metrics.export.enabled=true",
        "management.prometheus.scrape-token=observability-test-scrape-token",
        "batch.alrim-event.enabled=false",
        "batch.auradb-keep-alive.enabled=false"
    }
)
class ObservabilityHttpIntegrationTest {

    private static final String SCRAPE_TOKEN = "observability-test-scrape-token";

    @LocalServerPort
    private int applicationPort;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private RecommendationObservability recommendationObservability;

    @BeforeAll
    static void useLoopbackForHttpClient() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    @Test
    void managementPortExposesPrometheusMetricsOnlyOnTheManagementBoundary() {
        ResponseEntity<String> unauthenticated = get(managementPort, "/actuator/prometheus");
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> wrongToken = getWithBearer(
            managementPort, "/actuator/prometheus", "wrong-scrape-token");
        assertThat(wrongToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders malformedHeaders = new HttpHeaders();
        malformedHeaders.set(HttpHeaders.AUTHORIZATION, "Basic not-a-bearer-token");
        ResponseEntity<String> malformedAuthorization = exchange(
            managementPort, "/actuator/prometheus", malformedHeaders);
        assertThat(malformedAuthorization.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> matrixParameterWithoutToken = get(
            managementPort, "/actuator/prometheus;probe=1");
        assertThat(matrixParameterWithoutToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> managementResponse = getWithBearer(
            managementPort, "/actuator/prometheus", SCRAPE_TOKEN);
        assertThat(managementResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(managementResponse.getHeaders().getContentType()).isNotNull();
        assertThat(managementResponse.getHeaders().getContentType().toString())
            .contains("text/plain");
        assertThat(managementResponse.getBody())
            .contains("jvm_memory_used_bytes")
            .contains("jvm_threads_live_threads")
            .contains("process_cpu_usage")
            .contains("process_resident_memory_bytes")
            .contains("hikaricp_connections_active");

        ResponseEntity<String> publicResponse = getWithBearer(
            applicationPort, "/actuator/prometheus", SCRAPE_TOKEN);
        assertThat(publicResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void httpRequestMetricsUseTheNormalizedSpringRoute() {
        ResponseEntity<String> apiResponse = get(applicationPort, "/api/v1/jobs");
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String metrics = getWithBearer(
            managementPort, "/actuator/prometheus", SCRAPE_TOKEN).getBody();
        assertThat(metrics)
            .contains("http_server_requests_seconds_count")
            .contains("uri=\"/api/v1/jobs\"");
    }

    @Test
    void recommendationMetricsExposeOnlyTheAllowlistedPrometheusFamilies() {
        recommendationObservability.completed(RecommendationEngineType.MYSQL, "v2", 3, 1_000L);

        String metrics = getWithBearer(
            managementPort, "/actuator/prometheus", SCRAPE_TOKEN).getBody();
        assertThat(metrics)
            .contains("mapleland_recommendation_requests_total")
            .contains("mapleland_recommendation_results_recommendations_count")
            .contains("mapleland_recommendation_results_recommendations_sum")
            .contains("mapleland_recommendation_results_recommendations_max")
            .contains("api_version=\"v2\"")
            .contains("engine=\"mysql\"")
            .contains("outcome=\"success\"");
    }

    @Test
    void publicPortDoesNotExposeManagementEndpointsOrHealthDetails() {
        assertThat(get(applicationPort, "/actuator/env").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(applicationPort, "/actuator/configprops").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(applicationPort, "/actuator/health").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> health = get(managementPort, "/actuator/health");
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).isEqualTo("{\"status\":\"UP\"}");
    }

    private ResponseEntity<String> get(int port, String path) {
        return http.getForEntity("http://127.0.0.1:" + port + path, String.class);
    }

    private ResponseEntity<String> getWithBearer(int port, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return exchange(port, path, headers);
    }

    private ResponseEntity<String> exchange(int port, String path, HttpHeaders headers) {
        return http.exchange(
            "http://127.0.0.1:" + port + path,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);
    }
}
