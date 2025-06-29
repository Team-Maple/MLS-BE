package com.maple.api.quest.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Quest {
    @Id
    @Column(name = "quest_id")
    private Integer questId;

    @Nullable
    @Column(name = "title_prefix")
    private String titlePrefix;

    @Column(name = "name_kr")
    private String nameKr;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "quest_type")
    private String questType;

    @Column(name = "min_level")
    private Integer minLevel;

    @Nullable
    @Column(name = "max_level")
    private Integer maxLevel;

    @Column(name = "required_meso_start")
    private Integer requiredMesoStart;

    @Column(name = "start_npc_id")
    private Integer startNpcId;

    @Column(name = "end_npc_id")
    private Integer endNpcId;


    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}