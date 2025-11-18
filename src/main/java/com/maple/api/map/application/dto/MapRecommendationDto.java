package com.maple.api.map.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사냥터 추천 결과")
public record MapRecommendationDto(
        @Schema(description = "추천 맵 ID", example = "100000000")
        Integer mapId,

        @Schema(description = "레벨/직업 가중치를 반영한 최종 점수", example = "8.0")
        double score
) {
}
