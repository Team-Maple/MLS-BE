package com.maple.api.map.recommendation.application;

import com.maple.api.map.recommendation.port.RecommendationEngineType;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationObservabilityTest {

    @Test
    void exposesOnlyBoundedEngineApiVersionAndOutcomeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RecommendationObservability observability = new RecommendationObservability(registry);

        observability.completed(RecommendationEngineType.MYSQL, "v2", 0, 100L);
        observability.completed(RecommendationEngineType.MYSQL, "v2", 3, 200L);
        observability.disabled(RecommendationEngineType.MYSQL, "v2");
        observability.unavailable(RecommendationEngineType.AURA, "v1", 300L, new RuntimeException("down"));

        assertThat(registry.get(RecommendationObservability.REQUESTS_METRIC)
                .tags("engine", "mysql", "api_version", "v2", "outcome", "empty")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(RecommendationObservability.REQUESTS_METRIC)
                .tags("engine", "mysql", "api_version", "v2", "outcome", "success")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(RecommendationObservability.REQUESTS_METRIC)
                .tags("engine", "mysql", "api_version", "v2", "outcome", "unavailable")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(RecommendationObservability.REQUESTS_METRIC)
                .tags("engine", "aura", "api_version", "v1", "outcome", "unavailable")
                .counter().count()).isEqualTo(1.0d);
        assertThat(registry.get(RecommendationObservability.RESULTS_METRIC)
                .tags("engine", "mysql", "api_version", "v2")
                .summary().count()).isEqualTo(2L);
        assertThat(registry.get(RecommendationObservability.RESULTS_METRIC)
                .tags("engine", "mysql", "api_version", "v2")
                .summary().totalAmount()).isEqualTo(3.0d);

        Set<String> allowedTags = Set.of("engine", "api_version", "outcome");
        assertThat(registry.getMeters())
                .flatMap(meter -> meter.getId().getTags())
                .extracting(io.micrometer.core.instrument.Tag::getKey)
                .allMatch(allowedTags::contains);
        assertThat(registry.getMeters())
                .extracting(Meter::getId)
                .extracting(Meter.Id::getName)
                .allMatch(name -> name.startsWith("mapleland.recommendation."));
    }
}
