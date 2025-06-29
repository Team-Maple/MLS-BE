package com.maple.api.quest.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quest_requirements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestRequirement extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "quest_id")
    private Integer questId;

    @NotBlank
    @Column(name = "requirement_type")
    private String requirementType;

    @Nullable
    @Column(name = "item_id")
    private Integer itemId;

    @Nullable
    @Column(name = "monster_id")
    private Integer monsterId;

    @Min(0)
    @Column(name = "quantity")
    private Integer quantity;
}