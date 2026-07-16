package com.maple.api.common.presentation.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.env.MockEnvironment;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementPrometheusSecurityFilterTest {

    private static final int APPLICATION_PORT = 8080;
    private static final int MANAGEMENT_PORT = 18080;

    @Test
    void blankScrapeTokenFailsClosed() throws Exception {
        ManagementPrometheusSecurityFilter filter =
            filterWithToken("   ");
        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setLocalPort(MANAGEMENT_PORT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
            chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void blankScrapeTokenDoesNotBlockUnrelatedPaths() throws Exception {
        ManagementPrometheusSecurityFilter filter =
            filterWithToken("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs");
        request.setLocalPort(APPLICATION_PORT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
            chainCalled.set(true));

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void actuatorPathsOnTheApplicationPortAreHiddenAsNotFound() throws Exception {
        ManagementPrometheusSecurityFilter filter = filterWithToken("scrape-token");
        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setLocalPort(APPLICATION_PORT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
            chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void matrixParameterPrometheusPathStillRequiresBearerAuthentication() throws Exception {
        ManagementPrometheusSecurityFilter filter = filterWithToken("scrape-token");
        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/actuator/prometheus;probe=1");
        request.setLocalPort(MANAGEMENT_PORT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
            chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void decodedPrometheusPathStillRequiresBearerAuthentication() throws Exception {
        ManagementPrometheusSecurityFilter filter = filterWithToken("scrape-token");
        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/actuator/%70rometheus");
        request.setLocalPort(MANAGEMENT_PORT);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
            chainCalled.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainCalled).isFalse();
    }

    private ManagementPrometheusSecurityFilter filterWithToken(String token) {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("local.management.port", Integer.toString(MANAGEMENT_PORT));
        return new ManagementPrometheusSecurityFilter(token, environment);
    }
}
