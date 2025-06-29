package com.maple.api.item.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "item")
    private List<ItemJob> itemsJobs = new ArrayList<>();
}