package com.maple.api.monster.application.dto;

import com.maple.api.monster.domain.Monster;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "몬스터 상세 정보 응답")
public record MonsterDetailDto(
        @Schema(description = "몬스터 ID", example = "100100")
        Integer monsterId,
        
        @Schema(description = "몬스터 한국어 이름", example = "달팽이")
        String nameKr,
        
        @Schema(description = "몬스터 영어 이름", example = "Snail")
        String nameEn,
        
        @Schema(description = "몬스터 이미지 URL", example = "https://maplestory.io/api/gms/62/mob/100100/render/stand")
        String imageUrl,
        
        @Schema(description = "몬스터 레벨", example = "1")
        Integer level,
        
        @Schema(description = "몬스터 경험치", example = "3")
        Integer exp,
        
        @Schema(description = "몬스터 HP", example = "8")
        Integer hp,
        
        @Schema(description = "몬스터 MP", example = "0")
        Integer mp,
        
        @Schema(description = "몬스터 물리 방어력", example = "0")
        Integer physicalDefense,
        
        @Schema(description = "몬스터 마법 방어력", example = "0")
        Integer magicDefense,
        
        @Schema(description = "몬스터 명중률", example = "20")
        Integer requiredAccuracy,
        
        @Schema(description = "레벨 차이당 보너스 명중률", example = "0.0")
        Double bonusAccuracyPerLevelLower,
        
        @Schema(description = "몬스터 회피율", example = "0")
        Integer evasionRate,
        
        @Schema(description = "메소 드롭량", example = "5")
        Integer mesoDropAmount,
        
        @Schema(description = "메소 드롭 확률", example = "65")
        Integer mesoDropRate,
        
        @Schema(description = "속성 효과 정보")
        MonsterTypeEffectivenessDto typeEffectiveness,

        @Schema(description = "로그인 사용자가 생성한 북마크 ID (없으면 null)", example = "123")
        Integer bookmarkId
) {
    public static MonsterDetailDto toDto(
            Monster monster,
            MonsterTypeEffectivenessDto typeEffectiveness
    ) {
        return new MonsterDetailDto(
                monster.getMonsterId(),
                monster.getNameKr(),
                monster.getNameEn(),
                monster.getImageUrl(),
                monster.getLevel(),
                monster.getExp(),
                monster.getHp(),
                monster.getMp(),
                monster.getPhysicalDefense(),
                monster.getMagicDefense(),
                monster.getRequiredAccuracy(),
                monster.getBonusAccuracyPerLevelLower(),
                monster.getEvasionRate(),
                monster.getMesoDropAmount(),
                monster.getMesoDropRate(),
                typeEffectiveness,
                null
        );
    }

    public static MonsterDetailDto toDto(
            Monster monster,
            MonsterTypeEffectivenessDto typeEffectiveness,
            Integer bookmarkId
    ) {
        return new MonsterDetailDto(
                monster.getMonsterId(),
                monster.getNameKr(),
                monster.getNameEn(),
                monster.getImageUrl(),
                monster.getLevel(),
                monster.getExp(),
                monster.getHp(),
                monster.getMp(),
                monster.getPhysicalDefense(),
                monster.getMagicDefense(),
                monster.getRequiredAccuracy(),
                monster.getBonusAccuracyPerLevelLower(),
                monster.getEvasionRate(),
                monster.getMesoDropAmount(),
                monster.getMesoDropRate(),
                typeEffectiveness,
                bookmarkId
        );
    }
}
