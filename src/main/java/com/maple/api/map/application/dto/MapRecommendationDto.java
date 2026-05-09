package com.maple.api.map.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사냥터 추천 결과")
public record MapRecommendationDto(
        @Schema(description = "추천 맵 ID", example = "100000000")
        Integer mapId,

        @Schema(description = "레벨/직업 가중치를 반영한 최종 점수", example = "8.0")
        double score,

        @Schema(description = "맵 아이콘 URL", example = "https://maplestory.io/api/gms/62/map/100000000/icon?resize=2")
        String iconUrl,

        @Schema(description = "한국어 맵 이름", example = "헤네시스")
        String nameKr,

        @Schema(description = "로그인 사용자가 생성한 북마크 ID (없으면 null)", example = "123")
        Integer bookmarkId
) {
    public MapRecommendationDto(Integer mapId, double score) {
        this(mapId, score, null, null, null);
    }
}
