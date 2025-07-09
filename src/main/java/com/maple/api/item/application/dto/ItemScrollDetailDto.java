package com.maple.api.item.application.dto;

import com.maple.api.item.domain.ItemScrollDetail;

public record ItemScrollDetailDto(
        Integer successRatePercent,
        String targetItemTypeText,
        Integer strChange,
        Integer dexChange,
        Integer intChange,
        Integer lukChange,
        Integer hpChange,
        Integer mpChange,
        Integer weaponAttackChange,
        Integer magicAttackChange,
        Integer physicalDefenseChange,
        Integer magicDefenseChange,
        Integer accuracyChange,
        Integer evasionChange,
        Integer speedChange,
        Integer jumpChange
) {
    public static ItemScrollDetailDto toDto(ItemScrollDetail scrollDetail) {
        if (scrollDetail == null) {
            return null;
        }
        
        return new ItemScrollDetailDto(
                scrollDetail.getSuccessRatePercent(),
                scrollDetail.getTargetItemTypeText(),
                scrollDetail.getStrChange(),
                scrollDetail.getDexChange(),
                scrollDetail.getIntChange(),
                scrollDetail.getLukChange(),
                scrollDetail.getHpChange(),
                scrollDetail.getMpChange(),
                scrollDetail.getWeaponAttackChange(),
                scrollDetail.getMagicAttackChange(),
                scrollDetail.getPhysicalDefenseChange(),
                scrollDetail.getMagicDefenseChange(),
                scrollDetail.getAccuracyChange(),
                scrollDetail.getEvasionChange(),
                scrollDetail.getSpeedChange(),
                scrollDetail.getJumpChange()
        );
    }
}