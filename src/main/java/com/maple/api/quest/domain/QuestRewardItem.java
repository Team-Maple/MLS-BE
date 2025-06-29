package com.maple.api.quest.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quest_reward_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestRewardItem extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "quantity")
    private Integer quantity;
}