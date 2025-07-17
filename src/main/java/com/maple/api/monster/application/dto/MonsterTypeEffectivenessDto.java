package com.maple.api.monster.application.dto;

import com.maple.api.monster.domain.MonsterTypeEffectiveness;
import com.maple.api.monster.domain.TypeEffectiveness;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "몬스터 속성 효과 정보")
public record MonsterTypeEffectivenessDto(
        @Schema(description = "불 속성 효과", example = "WEAK")
        TypeEffectiveness fire,
        
        @Schema(description = "번개 속성 효과", example = "RESIST")
        TypeEffectiveness lightning,
        
        @Schema(description = "독 속성 효과", example = "IMMUNE")
        TypeEffectiveness poison,
        
        @Schema(description = "신성 속성 효과", example = "WEAK")
        TypeEffectiveness holy,
        
        @Schema(description = "얼음 속성 효과", example = "RESIST")
        TypeEffectiveness ice,
        
        @Schema(description = "물리 속성 효과", example = "WEAK")
        TypeEffectiveness physical
) {
    public static MonsterTypeEffectivenessDto toDto(MonsterTypeEffectiveness effectiveness) {
        return new MonsterTypeEffectivenessDto(
                effectiveness.getFire(),
                effectiveness.getLightning(),
                effectiveness.getPoison(),
                effectiveness.getHoly(),
                effectiveness.getIce(),
                effectiveness.getPhysical()
        );
    }
}