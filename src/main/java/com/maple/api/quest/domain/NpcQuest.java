package com.maple.api.quest.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "npc_quests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NpcQuest {
    @Id
    private Integer id;

    @Column(name = "npc_id")
    private Integer npcId;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "quest_name")
    private String questName;

    @Column(name = "quest_icon_url")
    private String questIconUrl;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}