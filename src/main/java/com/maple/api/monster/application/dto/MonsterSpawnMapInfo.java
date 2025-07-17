package com.maple.api.monster.application.dto;

import com.maple.api.map.domain.Map;
import com.maple.api.map.domain.MonsterSpawnMap;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "몬스터 스폰 맵 정보")
public record MonsterSpawnMapInfo(
        @Schema(description = "맵 ID", example = "103000000")
        Integer mapId,
        
        @Schema(description = "맵 이름", example = "메이플로드: 달팽이동산")
        String mapName,
        
        @Schema(description = "지역 이름", example = "메이플로드")
        String regionName,
        
        @Schema(description = "상세 지역 이름", example = "달팽이동산")
        String detailName,
        
        @Schema(description = "최상위 지역 이름", example = "메이플로드")
        String topRegionName,
        
        @Schema(description = "맵 아이콘 URL", example = "https://maplestory.io/api/gms/62/map/20000/icon?resize=2")
        String iconUrl,
        
        @Schema(description = "최대 스폰 수", example = "10")
        Integer maxSpawnCount
) {
    public static MonsterSpawnMapInfo toDto(MonsterSpawnMap spawnMap, Map map) {
        return new MonsterSpawnMapInfo(
                spawnMap.getMapId(),
                map != null ? map.getNameKr() : null,
                map != null ? map.getRegionName() : null,
                map != null ? map.getDetailName() : null,
                map != null ? map.getTopRegionName() : null,
                map != null ? map.getIconUrl() : null,
                spawnMap.getMaxSpawnCount()
        );
    }
}