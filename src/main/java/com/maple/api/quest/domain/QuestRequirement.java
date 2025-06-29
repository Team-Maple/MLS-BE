package com.maple.api.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quest_requirements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestRequirement {
    @Id
    private Integer id;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "requirement_type")
    private String requirementType;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "monster_id")
    private Integer monsterId;

    @Column(name = "quantity")
    private Integer quantity;
}