package com.maple.api.map.recommendation.config;

import com.maple.api.map.recommendation.port.RecommendationEngineType;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    private String v1Engine = RecommendationEngineType.AURA.name();

    private boolean v2Enabled = false;

    @Min(1)
    @Max(60)
    private int queryTimeoutSeconds = 10;
}
