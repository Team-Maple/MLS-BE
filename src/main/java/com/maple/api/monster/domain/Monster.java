package com.maple.api.monster.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monsters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Monster extends BaseEntity {
    @Id
    @Column(name = "monster_id")
    private Integer monsterId;

    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "level")
    private Integer level;

    @Column(name = "exp")
    private Integer exp;

    @Column(name = "hp")
    private Integer hp;

    @Column(name = "mp")
    private Integer mp;

    @Column(name = "physical_defense")
    private Integer physicalDefense;

    @Column(name = "magic_defense")
    private Integer magicDefense;

    @Column(name = "required_accuracy")
    private Integer requiredAccuracy;

    @Column(name = "bonus_accuracy_per_level_lower")
    private Double bonusAccuracyPerLevelLower;

    @Column(name = "evasion_rate")
    private Integer evasionRate;

    @Nullable
    @Column(name = "meso_drop_amount")
    private Integer mesoDropAmount;

    @Nullable
    @Column(name = "meso_drop_rate")
    private Integer mesoDropRate;
}