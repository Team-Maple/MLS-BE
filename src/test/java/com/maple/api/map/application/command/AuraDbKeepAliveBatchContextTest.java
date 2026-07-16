package com.maple.api.map.application.command;

import com.maple.api.map.repository.AuraMapRecommendationRepository;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuraDbKeepAliveBatchContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("batch.auradb-keep-alive.enabled=true")
            .withUserConfiguration(AuraComponents.class);

    @Test
    void createsRepositoryAndKeepAliveWhenDriverIsAvailable() {
        contextRunner
                .withBean(Driver.class, () -> mock(Driver.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuraMapRecommendationRepository.class);
                    assertThat(context).hasSingleBean(AuraDbKeepAliveBatch.class);
                });
    }

    @Test
    void startsWithoutAuraBeansWhenDriverIsUnavailable() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AuraMapRecommendationRepository.class);
            assertThat(context).doesNotHaveBean(AuraDbKeepAliveBatch.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({AuraMapRecommendationRepository.class, AuraDbKeepAliveBatch.class})
    static class AuraComponents {
    }
}
