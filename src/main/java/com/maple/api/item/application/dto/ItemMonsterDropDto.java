package com.maple.api.item.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이템을 드롭하는 몬스터 정보")
public record ItemMonsterDropDto(
        @Schema(description = "몬스터 ID", example = "7220001")
        Integer monsterId,
        
        @Schema(description = "몬스터 이름", example = "구미호")
        String monsterName,
        
        @Schema(description = "몬스터 레벨", example = "70")
        Integer level,
        
        @Schema(description = "드롭율", example = "0.1")
        Double dropRate,
        
        @Schema(description = "몬스터 이미지 URL")
        String imageUrl
) {
}