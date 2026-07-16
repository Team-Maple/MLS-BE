package com.maple.api.observability;

import ch.qos.logback.classic.LoggerContext;
import com.maple.api.common.logging.SafeExceptionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/** Runs the real Spring Boot logging bootstrap in an isolated JVM. */
public final class ProdLoggingForkFixture {

    private static final Logger log = LoggerFactory.getLogger(ProdLoggingForkFixture.class);
    static final String SENSITIVE_MEMBER_ID = "fork-member-id-that-must-not-leak";
    static final String SENSITIVE_ACCESS_TOKEN = "fork-access-token-that-must-not-leak";
    static final String SENSITIVE_AUTHORIZATION = "Bearer fork-authorization-that-must-not-leak";

    private ProdLoggingForkFixture() {
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LoggingFixtureConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);

        ConfigurableApplicationContext applicationContext = null;
        try {
            applicationContext = application.run(
                "--spring.profiles.active=prod",
                "--spring.main.banner-mode=off",
                "--logging.logback.rollingpolicy.max-file-size=4KB",
                "--logging.logback.rollingpolicy.max-history=2",
                "--logging.logback.rollingpolicy.total-size-cap=64KB");

            String payload = "x".repeat(512);
            for (int index = 0; index < 120; index++) {
                log.info("Rolling fixture event {} {}", index, payload);
            }
            SafeExceptionLog.addException(log.atError(), sensitiveFailure())
                .addKeyValue("event.action", "observability.safe-exception-fixture")
                .addKeyValue("event.outcome", "failure")
                .log("Production safe exception fixture failed");
            log.atWarn()
                .addKeyValue("event.action", "observability.prod-file-fixture")
                .addKeyValue("event.outcome", "success")
                .log("Production file logging fixture completed");
        } finally {
            if (applicationContext != null) {
                applicationContext.close();
            }
            if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
                loggerContext.stop();
            }
        }
    }

    private static Throwable sensitiveFailure() {
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
        return failure;
    }

    @Configuration(proxyBeanMethods = false)
    static class LoggingFixtureConfiguration {
    }
}
