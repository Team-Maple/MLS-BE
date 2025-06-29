package com.maple.api.item.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_equipments_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemEquipmentStats {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private EquipmentItem item;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "str_base")),
            @AttributeOverride(name = "min", column = @Column(name = "str_min")),
            @AttributeOverride(name = "max", column = @Column(name = "str_max"))
    })
    private StatRange str;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "dex_base")),
            @AttributeOverride(name = "min", column = @Column(name = "dex_min")),
            @AttributeOverride(name = "max", column = @Column(name = "dex_max"))
    })
    private StatRange dex;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "int_base")),
            @AttributeOverride(name = "min", column = @Column(name = "int_min")),
            @AttributeOverride(name = "max", column = @Column(name = "int_max"))
    })
    private StatRange intelligence;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "luk_base")),
            @AttributeOverride(name = "min", column = @Column(name = "luk_min")),
            @AttributeOverride(name = "max", column = @Column(name = "luk_max"))
    })
    private StatRange luk;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "hp_base")),
            @AttributeOverride(name = "min", column = @Column(name = "hp_min")),
            @AttributeOverride(name = "max", column = @Column(name = "hp_max"))
    })
    private StatRange hp;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "mp_base")),
            @AttributeOverride(name = "min", column = @Column(name = "mp_min")),
            @AttributeOverride(name = "max", column = @Column(name = "mp_max"))
    })
    private StatRange mp;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "weapon_attack_base")),
            @AttributeOverride(name = "min", column = @Column(name = "weapon_attack_min")),
            @AttributeOverride(name = "max", column = @Column(name = "weapon_attack_max"))
    })
    private StatRange weaponAttack;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "magic_attack_base")),
            @AttributeOverride(name = "min", column = @Column(name = "magic_attack_min")),
            @AttributeOverride(name = "max", column = @Column(name = "magic_attack_max"))
    })
    private StatRange magicAttack;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "physical_defense_base")),
            @AttributeOverride(name = "min", column = @Column(name = "physical_defense_min")),
            @AttributeOverride(name = "max", column = @Column(name = "physical_defense_max"))
    })
    private StatRange physicalDefense;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "magic_defense_base")),
            @AttributeOverride(name = "min", column = @Column(name = "magic_defense_min")),
            @AttributeOverride(name = "max", column = @Column(name = "magic_defense_max"))
    })
    private StatRange magicDefense;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "accuracy_base")),
            @AttributeOverride(name = "min", column = @Column(name = "accuracy_min")),
            @AttributeOverride(name = "max", column = @Column(name = "accuracy_max"))
    })
    private StatRange accuracy;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "evasion_base")),
            @AttributeOverride(name = "min", column = @Column(name = "evasion_min")),
            @AttributeOverride(name = "max", column = @Column(name = "evasion_max"))
    })
    private StatRange evasion;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "speed_base")),
            @AttributeOverride(name = "min", column = @Column(name = "speed_min")),
            @AttributeOverride(name = "max", column = @Column(name = "speed_max"))
    })
    private StatRange speed;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "base", column = @Column(name = "jump_base")),
            @AttributeOverride(name = "min", column = @Column(name = "jump_min")),
            @AttributeOverride(name = "max", column = @Column(name = "jump_max"))
    })
    private StatRange jump;

    @Nullable
    @Column(name = "attack_speed")
    private Integer attackSpeed;

    @Nullable
    @Column(name = "attack_speed_details")
    private String attackSpeedDetails;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}