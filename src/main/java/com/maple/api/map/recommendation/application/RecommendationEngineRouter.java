package com.maple.api.map.recommendation.application;

import com.maple.api.map.recommendation.port.RecommendationEnginePort;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RecommendationEngineRouter {

    private final Map<RecommendationEngineType, RecommendationEnginePort> engines;

    public RecommendationEngineRouter(List<RecommendationEnginePort> enginePorts) {
        EnumMap<RecommendationEngineType, RecommendationEnginePort> registered =
                new EnumMap<>(RecommendationEngineType.class);
        for (RecommendationEnginePort enginePort : enginePorts) {
            RecommendationEnginePort previous = registered.put(enginePort.engineType(), enginePort);
            if (previous != null) {
                throw new IllegalStateException("Duplicate recommendation engine: " + enginePort.engineType());
            }
        }
        this.engines = Map.copyOf(registered);
    }

    public Optional<RecommendationEnginePort> find(RecommendationEngineType engineType) {
        return Optional.ofNullable(engines.get(engineType));
    }
}
