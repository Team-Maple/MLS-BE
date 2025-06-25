package com.maple.api.item.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_equipments_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ItemEquipmentStats {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "str_base")
    private Integer strBase;

    @Column(name = "str_min")
    private Integer strMin;

    @Column(name = "str_max")
    private Integer strMax;

    @Column(name = "dex_base")
    private Integer dexBase;

    @Column(name = "dex_min")
    private Integer dexMin;

    @Column(name = "dex_max")
    private Integer dexMax;

    @Column(name = "int_base")
    private Integer intBase;

    @Column(name = "int_min")
    private Integer intMin;

    @Column(name = "int_max")
    private Integer intMax;

    @Column(name = "luk_base")
    private Integer lukBase;

    @Column(name = "luk_min")
    private Integer lukMin;

    @Column(name = "luk_max")
    private Integer lukMax;

    @Column(name = "hp_base")
    private Integer hpBase;

    @Column(name = "hp_min")
    private Integer hpMin;

    @Column(name = "hp_max")
    private Integer hpMax;

    @Column(name = "mp_base")
    private Integer mpBase;

    @Column(name = "mp_min")
    private Integer mpMin;

    @Column(name = "mp_max")
    private Integer mpMax;

    @Column(name = "weapon_attack_base")
    private Integer weaponAttackBase;

    @Column(name = "weapon_attack_min")
    private Integer weaponAttackMin;

    @Column(name = "weapon_attack_max")
    private Integer weaponAttackMax;

    @Column(name = "magic_attack_base")
    private Integer magicAttackBase;

    @Column(name = "magic_attack_min")
    private Integer magicAttackMin;

    @Column(name = "magic_attack_max")
    private Integer magicAttackMax;

    @Column(name = "physical_defense_base")
    private Integer physicalDefenseBase;

    @Column(name = "physical_defense_min")
    private Integer physicalDefenseMin;

    @Column(name = "physical_defense_max")
    private Integer physicalDefenseMax;

    @Column(name = "magic_defense_base")
    private Integer magicDefenseBase;

    @Column(name = "magic_defense_min")
    private Integer magicDefenseMin;

    @Column(name = "magic_defense_max")
    private Integer magicDefenseMax;

    @Column(name = "accuracy_base")
    private Integer accuracyBase;

    @Column(name = "accuracy_min")
    private Integer accuracyMin;

    @Column(name = "accuracy_max")
    private Integer accuracyMax;

    @Column(name = "evasion_base")
    private Integer evasionBase;

    @Column(name = "evasion_min")
    private Integer evasionMin;

    @Column(name = "evasion_max")
    private Integer evasionMax;

    @Column(name = "speed_base")
    private Integer speedBase;

    @Column(name = "speed_min")
    private Integer speedMin;

    @Column(name = "speed_max")
    private Integer speedMax;

    @Column(name = "jump_base")
    private Integer jumpBase;

    @Column(name = "jump_min")
    private Integer jumpMin;

    @Column(name = "jump_max")
    private Integer jumpMax;

    @Column(name = "attack_speed")
    private Integer attackSpeed;

    @Column(name = "attack_speed_details")
    private String attackSpeedDetails;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}