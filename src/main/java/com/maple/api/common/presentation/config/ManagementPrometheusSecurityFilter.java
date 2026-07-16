package com.maple.api.common.presentation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ManagementPrometheusSecurityFilter extends OncePerRequestFilter {

    private static final String ACTUATOR_PATH_PREFIX = "/actuator/";
    private static final String PROMETHEUS_PATH = "/actuator/prometheus";
    private static final String BEARER_PREFIX = "Bearer ";
    private final byte[] expectedToken;
    private final Environment environment;

    public ManagementPrometheusSecurityFilter(
        @Value("${management.prometheus.scrape-token:}") String expectedToken,
        Environment environment) {
        this.expectedToken = expectedToken == null || expectedToken.isBlank()
            ? new byte[0]
            : expectedToken.getBytes(StandardCharsets.UTF_8);
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !requestPath(request).startsWith(ACTUATOR_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String requestPath = requestPath(request);
        if (!isManagementPort(request)) {
            // Set the status directly so an error dispatch cannot be rewritten
            // by the application's authentication entry point into a 401.
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!PROMETHEUS_PATH.equals(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedToken.length == 0) {
            response.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Prometheus scrape authentication is not configured");
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        byte[] providedToken = authorization != null && authorization.startsWith(BEARER_PREFIX)
            ? authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8)
            : new byte[0];

        if (!MessageDigest.isEqual(expectedToken, providedToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isManagementPort(HttpServletRequest request) {
        Integer managementPort = environment.getProperty("local.management.port", Integer.class);
        if (managementPort == null || managementPort <= 0) {
            managementPort = environment.getProperty("management.server.port", Integer.class);
        }
        return managementPort != null
            && managementPort > 0
            && request.getLocalPort() == managementPort;
    }

    private String requestPath(HttpServletRequest request) {
        // Match Spring MVC's decoded, semicolon-stripped lookup path. Using the
        // raw request URI here would let a matrix-parameter variant reach the
        // same Actuator endpoint without taking the bearer-authentication path.
        return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    }
}
