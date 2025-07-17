package com.maple.api.monster.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monster_type_effectiveness")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MonsterTypeEffectiveness extends BaseEntity {
    @Id
    @Column(name = "monster_id")
    private Integer monsterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fire", length = 10)
    private TypeEffectiveness fire;

    @Enumerated(EnumType.STRING)
    @Column(name = "lightning", length = 10)
    private TypeEffectiveness lightning;

    @Enumerated(EnumType.STRING)
    @Column(name = "poison", length = 10)
    private TypeEffectiveness poison;

    @Enumerated(EnumType.STRING)
    @Column(name = "holy", length = 10)
    private TypeEffectiveness holy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ice", length = 10)
    private TypeEffectiveness ice;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical", length = 10)
    private TypeEffectiveness physical;
}
