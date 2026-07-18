package com.maple.api.common.config;

import com.maple.api.map.domain.RecommendationScoringService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(RecommendationProperties.class)
public class RecommendationConfig {

    @Bean
    public RecommendationScoringService recommendationScoringService() {
        return new RecommendationScoringService();
    }

    @Bean
    @Qualifier("recommendationJdbcTemplate")
    public NamedParameterJdbcTemplate recommendationJdbcTemplate(
            DataSource dataSource,
            RecommendationProperties properties
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(properties.getQueryTimeoutSeconds());
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }
}
