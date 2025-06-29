package com.maple.api.quest.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "npc_quests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NpcQuest extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "npc_id")
    private Integer npcId;

    @Nullable
    @Column(name = "quest_id")
    private Integer questId;

    @Nullable
    @Column(name = "quest_name")
    private String questName;

    @Column(name = "quest_icon_url")
    private String questIconUrl;
}