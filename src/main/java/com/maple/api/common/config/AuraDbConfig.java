package com.maple.api.common.config;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(AuraDbProperties.class)
@RequiredArgsConstructor
public class AuraDbConfig {

    private final AuraDbProperties auraDbProperties;

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("'${auradb.uri:}' != ''")
    public Driver auraDbDriver() {
        Assert.hasText(auraDbProperties.getUri(), "AuraDB uri must not be empty");
        Assert.hasText(auraDbProperties.getUsername(), "AuraDB username must not be empty");
        Assert.hasText(auraDbProperties.getPassword(), "AuraDB password must not be empty");

        Config driverConfig = Config.builder()
                .withMaxConnectionPoolSize(20)
                .build();

        return GraphDatabase.driver(
                auraDbProperties.getUri(),
                AuthTokens.basic(auraDbProperties.getUsername(), auraDbProperties.getPassword()),
                driverConfig
        );
    }
}
