package com.maple.api.item.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_scrolls_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemScrollDetail {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private ScrollItem item;

    @Nullable
    @Column(name = "success_rate_percent")
    private Integer successRatePercent;

    @Column(name = "target_item_type_text")
    private String targetItemTypeText;

    @Column(name = "str_change")
    private Integer strChange;

    @Column(name = "dex_change")
    private Integer dexChange;

    @Column(name = "int_change")
    private Integer intChange;

    @Column(name = "luk_change")
    private Integer lukChange;

    @Column(name = "hp_change")
    private Integer hpChange;

    @Column(name = "mp_change")
    private Integer mpChange;

    @Column(name = "weapon_attack_change")
    private Integer weaponAttackChange;

    @Column(name = "magic_attack_change")
    private Integer magicAttackChange;

    @Column(name = "physical_defense_change")
    private Integer physicalDefenseChange;

    @Column(name = "magic_defense_change")
    private Integer magicDefenseChange;

    @Column(name = "accuracy_change")
    private Integer accuracyChange;

    @Column(name = "evasion_change")
    private Integer evasionChange;

    @Column(name = "speed_change")
    private Integer speedChange;

    @Column(name = "jump_change")
    private Integer jumpChange;

    @Column(name = "effect_description")
    private String effectDescription;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}