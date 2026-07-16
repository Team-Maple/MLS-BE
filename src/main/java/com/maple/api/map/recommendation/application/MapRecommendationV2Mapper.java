package com.maple.api.map.recommendation.application;

import com.maple.api.map.application.dto.MapRecommendationReasonDto;
import com.maple.api.map.application.dto.MapRecommendationV2Dto;
import org.springframework.stereotype.Component;

@Component
public class MapRecommendationV2Mapper {

    public MapRecommendationV2Dto toDto(EnrichedRecommendationCandidate candidate) {
        return new MapRecommendationV2Dto(
                candidate.mapId(),
                candidate.score(),
                candidate.iconUrl(),
                candidate.nameKr(),
                candidate.bookmarkId(),
                candidate.reasons().stream()
                        .map(reason -> new MapRecommendationReasonDto(reason.axis(), reason.value()))
                        .toList()
        );
    }
}
