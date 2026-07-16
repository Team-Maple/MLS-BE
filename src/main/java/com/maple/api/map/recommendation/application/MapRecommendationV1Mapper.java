package com.maple.api.map.recommendation.application;

import com.maple.api.map.application.dto.MapRecommendationDto;
import org.springframework.stereotype.Component;

@Component
public class MapRecommendationV1Mapper {

    public MapRecommendationDto toDto(EnrichedRecommendationCandidate candidate) {
        return new MapRecommendationDto(
                candidate.mapId(),
                candidate.score(),
                candidate.iconUrl(),
                candidate.nameKr(),
                candidate.bookmarkId()
        );
    }
}
