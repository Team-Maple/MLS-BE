package com.maple.api.item.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Item {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "description_text")
    private String descriptionText;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "item_image_url")
    private String itemImageUrl;

    @Column(name = "npc_price")
    private Integer npcPrice;

    @Column(name = "required_level")
    private Integer requiredLevel;

    @Column(name = "required_str")
    private Integer requiredStr;

    @Column(name = "required_dex")
    private Integer requiredDex;

    @Column(name = "required_int")
    private Integer requiredInt;

    @Column(name = "required_luk")
    private Integer requiredLuk;

    @Column(name = "required_pop")
    private Integer requiredPop;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}