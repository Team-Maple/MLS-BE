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
                nullIfZero(scrollDetail.getSuccessRatePercent()),
                scrollDetail.getTargetItemTypeText(),
                nullIfZero(scrollDetail.getStrChange()),
                nullIfZero(scrollDetail.getDexChange()),
                nullIfZero(scrollDetail.getIntChange()),
                nullIfZero(scrollDetail.getLukChange()),
                nullIfZero(scrollDetail.getHpChange()),
                nullIfZero(scrollDetail.getMpChange()),
                nullIfZero(scrollDetail.getWeaponAttackChange()),
                nullIfZero(scrollDetail.getMagicAttackChange()),
                nullIfZero(scrollDetail.getPhysicalDefenseChange()),
                nullIfZero(scrollDetail.getMagicDefenseChange()),
                nullIfZero(scrollDetail.getAccuracyChange()),
                nullIfZero(scrollDetail.getEvasionChange()),
                nullIfZero(scrollDetail.getSpeedChange()),
                nullIfZero(scrollDetail.getJumpChange())
        );
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
