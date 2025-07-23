package com.maple.api.map.application.dto;

import com.maple.api.map.domain.MonsterSpawnMap;
import com.maple.api.monster.domain.Monster;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맵 출현 몬스터 정보")
public record MapMonsterDto(
        @Schema(description = "몬스터 ID", example = "3210800")
        Integer monsterId,
        
        @Schema(description = "몬스터 이름", example = "루팡")
        String monsterName,
        
        @Schema(description = "몬스터 레벨", example = "37")
        Integer level,
        
        @Schema(description = "최대 스폰 수", example = "20")
        Integer maxSpawnCount,
        
        @Schema(description = "몬스터 이미지 URL")
        String imageUrl
) {
    public static MapMonsterDto toDto(MonsterSpawnMap spawnMap, Monster monster) {
        return new MapMonsterDto(
                monster.getMonsterId(),
                monster.getNameKr(),
                monster.getLevel(),
                spawnMap.getMaxSpawnCount(),
                monster.getImageUrl()
        );
    }
}