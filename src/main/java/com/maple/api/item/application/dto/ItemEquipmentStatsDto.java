package com.maple.api.item.application.dto;

import com.maple.api.item.domain.ItemEquipmentStats;

public record ItemEquipmentStatsDto(
        StatRangeDto str,
        StatRangeDto dex,
        StatRangeDto intelligence,
        StatRangeDto luk,
        StatRangeDto hp,
        StatRangeDto mp,
        StatRangeDto weaponAttack,
        StatRangeDto magicAttack,
        StatRangeDto physicalDefense,
        StatRangeDto magicDefense,
        StatRangeDto accuracy,
        StatRangeDto evasion,
        StatRangeDto speed,
        StatRangeDto jump,
        Integer attackSpeed,
        String attackSpeedDetails
) {
    public static ItemEquipmentStatsDto toDto(ItemEquipmentStats equipmentStats) {
        if (equipmentStats == null) {
            return null;
        }
        
        return new ItemEquipmentStatsDto(
                StatRangeDto.toDto(equipmentStats.getStr()),
                StatRangeDto.toDto(equipmentStats.getDex()),
                StatRangeDto.toDto(equipmentStats.getIntelligence()),
                StatRangeDto.toDto(equipmentStats.getLuk()),
                StatRangeDto.toDto(equipmentStats.getHp()),
                StatRangeDto.toDto(equipmentStats.getMp()),
                StatRangeDto.toDto(equipmentStats.getWeaponAttack()),
                StatRangeDto.toDto(equipmentStats.getMagicAttack()),
                StatRangeDto.toDto(equipmentStats.getPhysicalDefense()),
                StatRangeDto.toDto(equipmentStats.getMagicDefense()),
                StatRangeDto.toDto(equipmentStats.getAccuracy()),
                StatRangeDto.toDto(equipmentStats.getEvasion()),
                StatRangeDto.toDto(equipmentStats.getSpeed()),
                StatRangeDto.toDto(equipmentStats.getJump()),
                equipmentStats.getAttackSpeed(),
                equipmentStats.getAttackSpeedDetails()
        );
    }
}