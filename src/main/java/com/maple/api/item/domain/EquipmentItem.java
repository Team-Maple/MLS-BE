package com.maple.api.item.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("EQUIPMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentItem extends Item {

    @Embedded
    private RequiredStats requiredStats;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ItemEquipmentStats equipmentStats;

}