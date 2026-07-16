package com.maple.api.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.api.common.logging.SafeExceptionLog;
import com.maple.api.common.presentation.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EcsStructuredLoggingTest {

    private static final String SENSITIVE_MEMBER_ID = "member-id-that-must-not-leak";
    private static final String SENSITIVE_AUTHORIZATION = "Bearer token-that-must-not-leak";
    private static final String SENSITIVE_ACCESS_TOKEN = "access-token-that-must-not-leak";
    private static final int MAX_SAFE_STACK_TRACE_CHARS = 16 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LoggerContext context;
    private StructuredLogEncoder encoder;

    @BeforeEach
    void setUp() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.application.name", "mapleland-api")
            .withProperty("spring.application.pid", "12345")
            .withProperty("logging.structured.ecs.service.environment", "prod")
            .withProperty("logging.structured.ecs.service.version", "test-version");

        context = new LoggerContext();
        context.putObject(Environment.class.getName(), environment);

        encoder = new StructuredLogEncoder();
        encoder.setContext(context);
        encoder.setFormat("ecs");
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        encoder.stop();
        context.stop();
    }

    @Test
    void everyPhysicalLineIsOneValidEcsJsonEventWithStructuredFields() throws Exception {
        LoggingEvent event = event(Level.WARN, "External integration request failed");
        event.setKeyValuePairs(List.of(
            new KeyValuePair("event.action", "external.request"),
            new KeyValuePair("event.outcome", "failure"),
            new KeyValuePair("http.request.method", "POST"),
            new KeyValuePair("http.response.status_code", 502)
        ));

        String encoded = encode(event);
        assertThat(encoded.lines()).hasSize(1);

        JsonNode json = objectMapper.readTree(encoded);
        assertThat(json.path("@timestamp").asText()).isNotBlank();
        assertThat(json.path("log.level").asText()).isEqualTo("WARN");
        assertThat(json.path("log.logger").asText()).isEqualTo("com.maple.api.ExternalClient");
        assertThat(json.path("process.pid").isNumber()).isTrue();
        assertThat(json.path("process.thread.name").asText()).isEqualTo("observability-test");
        assertThat(json.path("service.name").asText()).isEqualTo("mapleland-api");
        assertThat(json.path("service.environment").asText()).isEqualTo("prod");
        assertThat(json.path("service.version").asText()).isEqualTo("test-version");
        assertThat(json.path("message").asText()).isEqualTo("External integration request failed");
        assertThat(json.path("event.action").asText()).isEqualTo("external.request");
        assertThat(json.path("http.response.status_code").asInt()).isEqualTo(502);
    }

    @Test
    void exceptionStackTraceIsEscapedInsideOneJsonEvent() throws Exception {
        IllegalStateException failure = new IllegalStateException("upstream unavailable");
        LoggingEvent event = event(Level.ERROR, "External integration failed");
        event.setThrowableProxy(new ThrowableProxy(failure));

        String encoded = encode(event);
        assertThat(encoded.lines()).hasSize(1);

        JsonNode json = objectMapper.readTree(encoded);
        assertThat(json.path("error.type").asText()).isEqualTo(IllegalStateException.class.getName());
        assertThat(json.path("error.message").asText()).isEqualTo("upstream unavailable");
        assertThat(json.path("error.stack_trace").asText())
            .contains("IllegalStateException")
            .contains("upstream unavailable");
    }

    @Test
    void safeExceptionLogRedactsMessagesWhilePreservingTypeRootCauseAndApplicationFrame()
        throws Exception {
        IllegalArgumentException rootCause = new IllegalArgumentException(
            "memberId=" + SENSITIVE_MEMBER_ID + " accessToken=" + SENSITIVE_ACCESS_TOKEN);
        rootCause.setStackTrace(new StackTraceElement[] {
            new StackTraceElement(
                "com.maple.api.auth.application.AuthService",
                "reissue",
                "AuthService.java",
                55)
        });
        IllegalStateException failure = new IllegalStateException(
            "Authorization: " + SENSITIVE_AUTHORIZATION,
            rootCause);
        failure.setStackTrace(new StackTraceElement[] {
            new StackTraceElement(
                "com.maple.api.common.presentation.exception.GlobalExceptionHandler",
                "handleGenericException",
                "GlobalExceptionHandler.java",
                126)
        });

        String encoded = encode(captureSafeException(failure));
        assertThat(encoded.lines()).hasSize(1);
        assertThat(encoded)
            .doesNotContain(SENSITIVE_MEMBER_ID)
            .doesNotContain(SENSITIVE_ACCESS_TOKEN)
            .doesNotContain(SENSITIVE_AUTHORIZATION);

        JsonNode json = objectMapper.readTree(encoded);
        assertThat(json.path("error.type").asText())
            .isEqualTo(IllegalStateException.class.getName());
        assertThat(json.path("error.message").asText())
            .isEqualTo("Exception message redacted by logging policy");
        assertThat(json.path("error.stack_trace").asText())
            .contains(IllegalStateException.class.getName())
            .contains("Caused by: " + IllegalArgumentException.class.getName())
            .contains("com.maple.api.auth.application.AuthService.reissue(AuthService.java:55)");
    }

    @Test
    void safeExceptionLogCapsOversizedStackTraceInsideOneJsonLine() throws Exception {
        Throwable failure = oversizedFailure();

        String encoded = encode(captureSafeException(failure));
        JsonNode json = objectMapper.readTree(encoded);
        String safeStackTrace = json.path("error.stack_trace").asText();

        assertThat(encoded.lines()).hasSize(1);
        assertThat(safeStackTrace)
            .hasSizeLessThanOrEqualTo(MAX_SAFE_STACK_TRACE_CHARS)
            .contains("[7] type=" + IllegalArgumentException.class.getName())
            .contains("[7] application_frame=" + oversizedApplicationFrame(7, 0));
        for (int depth = 0; depth < 7; depth++) {
            assertThat(safeStackTrace)
                .contains("[" + depth + "] type=" + IllegalStateException.class.getName());
        }
        assertThat(encoded.getBytes(StandardCharsets.UTF_8).length).isLessThan(24 * 1024);
        assertThat(encoded)
            .doesNotContain(SENSITIVE_MEMBER_ID)
            .doesNotContain(SENSITIVE_ACCESS_TOKEN)
            .doesNotContain(SENSITIVE_AUTHORIZATION);
    }

    @Test
    void safeExceptionLogDoesNotReplaceTheOriginalFailureWithANullPointerException()
        throws Exception {
        JsonNode json = objectMapper.readTree(encode(captureSafeException(null)));

        assertThat(json.path("message").asText()).isEqualTo("Unexpected request failure");
        assertThat(json.has("error.type")).isFalse();
        assertThat(json.has("error.message")).isFalse();
        assertThat(json.has("error.stack_trace")).isFalse();
    }

    @Test
    void genericExceptionHandlerWritesOneErrorTypeMemberInTheEcsEvent() throws Exception {
        IllegalStateException failure = new IllegalStateException(
            "memberId=" + SENSITIVE_MEMBER_ID + " Authorization=" + SENSITIVE_AUTHORIZATION);
        failure.setStackTrace(new StackTraceElement[] {
            new StackTraceElement(
                "com.maple.api.auth.application.AuthService",
                "reissue",
                "AuthService.java",
                55)
        });

        ILoggingEvent event = captureGenericHandlerException(failure);
        assertThat(event.getKeyValuePairs())
            .filteredOn(pair -> "error.type".equals(pair.key))
            .hasSize(1);

        String encoded = encode(event);
        assertThat(encoded)
            .containsOnlyOnce("\"error.type\"")
            .doesNotContain(SENSITIVE_MEMBER_ID)
            .doesNotContain(SENSITIVE_AUTHORIZATION);

        JsonNode json = objectMapper.readTree(encoded);
        assertThat(json.path("error.type").asText())
            .isEqualTo(IllegalStateException.class.getName());
        assertThat(json.path("event.action").asText()).isEqualTo("http.request.failure");
        assertThat(json.path("http.request.method").asText()).isEqualTo("POST");
        assertThat(json.path("http.response.status_code").asInt()).isEqualTo(500);
        assertThat(json.path("http.route").asText()).isEqualTo("/api/v1/auth/reissue");
    }

    @Test
    void sensitiveValuesAndRequestContextDoNotLeakIntoTheNextEvent() throws Exception {
        MDC.setContextMap(Map.of("trace.id", "trace-for-first-request"));
        LoggingEvent first = event(Level.INFO, "Member security setting updated");
        first.setKeyValuePairs(List.of(new KeyValuePair("event.action", "member.security.update")));
        String firstEncoded = encode(first);

        MDC.clear();
        LoggingEvent second = event(Level.INFO, "Request completed");
        String secondEncoded = encode(second);

        assertThat(firstEncoded)
            .doesNotContain(SENSITIVE_MEMBER_ID)
            .doesNotContain(SENSITIVE_AUTHORIZATION);
        JsonNode secondJson = objectMapper.readTree(secondEncoded);
        assertThat(secondJson.has("trace.id")).isFalse();
        assertThat(secondJson.has("event.action")).isFalse();
    }

    @Test
    void productionConfigurationSelectsEcsFileOutputAndBoundedRotation() throws Exception {
        List<PropertySource<?>> documents = new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));

        assertThat(property(documents, "spring.application.name")).isEqualTo("mapleland-api");
        assertThat(property(documents, "logging.structured.format.file")).isEqualTo("ecs");
        assertThat(property(documents, "logging.structured.ecs.service.environment")).isEqualTo("prod");
        assertThat(property(documents, "logging.logback.rollingpolicy.max-file-size")).isEqualTo("10MB");
        assertThat(property(documents, "logging.logback.rollingpolicy.max-history")).isEqualTo(14);
        assertThat(property(documents, "logging.logback.rollingpolicy.total-size-cap")).isEqualTo("250MB");
        assertThat(getClass().getClassLoader().getResource("logback-spring.xml")).isNull();
    }

    private LoggingEvent event(Level level, String message) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setLoggerName("com.maple.api.ExternalClient");
        event.setLevel(level);
        event.setMessage(message);
        event.setThreadName("observability-test");
        event.setInstant(Instant.now());
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        event.setMDCPropertyMap(mdc != null ? mdc : Map.of());
        return event;
    }

    private ILoggingEvent captureSafeException(Throwable failure) {
        Logger logger = context.getLogger("com.maple.api.observability.SafeExceptionFixture");
        logger.setAdditive(false);
        logger.setLevel(Level.ERROR);

        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.setContext(context);
        events.start();
        logger.addAppender(events);
        try {
            SafeExceptionLog.addException(logger.atError(), failure)
                .log("Unexpected request failure");
            assertThat(events.list).hasSize(1);
            LoggingEvent captured = (LoggingEvent) events.list.getFirst();
            captured.setMDCPropertyMap(Map.of());
            return captured;
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    private ILoggingEvent captureGenericHandlerException(Exception failure) {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
        boolean additive = logger.isAdditive();
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.setAdditive(false);
        logger.addAppender(events);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/reissue");
        when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
            .thenReturn("/api/v1/auth/reissue");

        try {
            new GlobalExceptionHandler().handleGenericException(failure, request);
            assertThat(events.list).hasSize(1);
            LoggingEvent captured = (LoggingEvent) events.list.getFirst();
            captured.setMDCPropertyMap(Map.of());
            return captured;
        } finally {
            logger.detachAppender(events);
            logger.setAdditive(additive);
            events.stop();
        }
    }

    private Throwable oversizedFailure() {
        Throwable cause = null;
        for (int depth = 7; depth >= 0; depth--) {
            Throwable current = depth == 7
                ? new IllegalArgumentException(
                    "depth=" + depth
                        + " memberId=" + SENSITIVE_MEMBER_ID
                        + " accessToken=" + SENSITIVE_ACCESS_TOKEN
                        + " Authorization=" + SENSITIVE_AUTHORIZATION,
                    cause)
                : new IllegalStateException(
                    "depth=" + depth
                        + " memberId=" + SENSITIVE_MEMBER_ID
                        + " accessToken=" + SENSITIVE_ACCESS_TOKEN
                        + " Authorization=" + SENSITIVE_AUTHORIZATION,
                    cause);
            StackTraceElement[] frames = new StackTraceElement[40];
            for (int frame = 0; frame < frames.length; frame++) {
                frames[frame] = new StackTraceElement(
                    oversizedApplicationClass(depth),
                    "handleFailureAtBoundary" + frame + "WithPreservedApplicationContext",
                    "SafeExceptionFixture.java",
                    100 + frame);
            }
            current.setStackTrace(frames);
            cause = current;
        }
        return cause;
    }

    private String oversizedApplicationClass(int depth) {
        return "com.maple.api.observability.verylongpackage."
            + "SensitiveBoundarySegment".repeat(6)
            + depth;
    }

    private String oversizedApplicationFrame(int depth, int frame) {
        return new StackTraceElement(
            oversizedApplicationClass(depth),
            "handleFailureAtBoundary" + frame + "WithPreservedApplicationContext",
            "SafeExceptionFixture.java",
            100 + frame).toString();
    }

    private String encode(ILoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8).stripTrailing();
    }

    private Object property(List<PropertySource<?>> documents, String name) {
        return documents.stream()
            .map(document -> document.getProperty(name))
            .filter(value -> value != null)
            .findFirst()
            .orElse(null);
    }
}
