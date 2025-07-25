package com.maple.api.map.application.dto;

import com.maple.api.map.domain.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맵 요약 정보")
public record MapSummaryDto(
        @Schema(description = "맵 ID", example = "100000000")
        Integer mapId,
        
        @Schema(description = "맵 이름", example = "헤네시스")
        String name,
        
        @Schema(description = "맵 이미지 URL", example = "https://maplestory.io/api/gms/62/map/100000000/icon?resize=2")
        String imageUrl,
        
        @Schema(description = "데이터 타입 (고정값: 'map')", example = "map")
        String type
) {
    public static MapSummaryDto toDto(Map entity) {
        return new MapSummaryDto(entity.getMapId(), entity.getNameKr(), entity.getIconUrl(), "map");
    }
}