package com.maple.api.map.application;

import com.maple.api.map.domain.RecommendationEngineType;
import com.maple.api.map.repository.MapRecommendationRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MapRecommendationEngineRouter {

    private final Map<RecommendationEngineType, MapRecommendationRepository> repositories;

    public MapRecommendationEngineRouter(List<MapRecommendationRepository> repositories) {
        EnumMap<RecommendationEngineType, MapRecommendationRepository> registered =
                new EnumMap<>(RecommendationEngineType.class);
        for (MapRecommendationRepository repository : repositories) {
            MapRecommendationRepository previous = registered.put(repository.engineType(), repository);
            if (previous != null) {
                throw new IllegalStateException("Duplicate recommendation engine: " + repository.engineType());
            }
        }
        this.repositories = Map.copyOf(registered);
    }

    public Optional<MapRecommendationRepository> find(RecommendationEngineType engineType) {
        return Optional.ofNullable(repositories.get(engineType));
    }
}
