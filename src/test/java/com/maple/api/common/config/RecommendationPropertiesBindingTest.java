package com.maple.api.common.config;

import com.maple.api.map.domain.RecommendationEngineType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsSupportedEngineCaseInsensitively() {
        contextRunner
                .withPropertyValues("recommendation.v1-engine=mysql")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RecommendationProperties.class).getV1Engine())
                            .isEqualTo(RecommendationEngineType.MYSQL);
                });
    }

    @Test
    void rejectsUnsupportedEngineDuringConfigurationBinding() {
        contextRunner
                .withPropertyValues("recommendation.v1-engine=not-an-engine")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("recommendation.v1-engine");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RecommendationProperties.class)
    static class TestConfiguration {
    }
}
