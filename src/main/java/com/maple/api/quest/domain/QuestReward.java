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
@Table(name = "quest_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestReward extends BaseEntity {
    @Id
    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "exp")
    private Long exp;

    @Column(name = "meso")
    private Long meso;

    @Column(name = "popularity")
    private Integer popularity;
}