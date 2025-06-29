package com.maple.api.item.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Item extends BaseEntity {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Nullable
    @Column(name = "description_text")
    private String descriptionText;

    @Column(name = "item_image_url")
    private String itemImageUrl;

    @Column(name = "npc_price")
    private Integer npcPrice;

    @Column(name = "category_id")
    private Integer categoryId;
}