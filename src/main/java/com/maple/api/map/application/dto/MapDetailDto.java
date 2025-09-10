package com.maple.api.map.application.dto;

import com.maple.api.map.domain.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맵 상세 정보")
public record MapDetailDto(
        @Schema(description = "맵 ID", example = "100000000")
        Integer mapId,
        
        @Schema(description = "한글명", example = "헤네시스: 헤네시스")
        String nameKr,
        
        @Schema(description = "영문명", example = "Victoria Road: Henesys")
        String nameEn,
        
        @Schema(description = "지역명", example = "헤네시스")
        String regionName,
        
        @Schema(description = "세부 지역명", example = "헤네시스")
        String detailName,
        
        @Schema(description = "최상위 지역명", example = "헤네시스")
        String topRegionName,
        
        @Schema(description = "맵 이미지 URL")
        String mapUrl,
        
        @Schema(description = "아이콘 URL")
        String iconUrl,
        
        @Schema(description = "로그인 사용자의 북마크 여부", example = "false")
        boolean isBookmarked
) {
    public static MapDetailDto toDto(Map map) {
        return toDto(map, false);
    }

    public static MapDetailDto toDto(Map map, boolean isBookmarked) {
        return new MapDetailDto(
                map.getMapId(),
                map.getNameKr(),
                map.getNameEn(),
                map.getRegionName(),
                map.getDetailName(),
                map.getTopRegionName(),
                map.getMapUrl(),
                map.getIconUrl(),
                isBookmarked
        );
    }
}
