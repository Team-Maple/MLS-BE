package com.maple.api.npc.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NPC 출현 맵 정보")
public record NpcSpawnMapDto(
        @Schema(description = "맵 ID", example = "20000")
        Integer mapId,
        
        @Schema(description = "맵 이름", example = "메이플로드: 달팽이동산")
        String mapName,
        
        @Schema(description = "지역 이름(히든스트리트, 워닝스트리트, 던전 등 카테고리)", example = "메이플로드")
        String regionName,
        
        @Schema(description = "상세 지역 이름(보통 부르는 맵 이름)", example = "달팽이동산")
        String detailName,
        
        @Schema(description = "최상위 지역 이름(엘리니아와 같은 지역 이름)", example = "메이플로드")
        String topRegionName,
        
        @Schema(description = "맵 아이콘 URL", example = "https://maplestory.io/api/gms/62/map/20000/icon")
        String iconUrl
) {
}