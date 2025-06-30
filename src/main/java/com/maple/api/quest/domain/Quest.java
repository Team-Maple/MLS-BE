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
@Table(name = "quests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Quest extends BaseEntity {
    @Id
    @Column(name = "quest_id")
    private Integer questId;

    @Nullable
    @Column(name = "title_prefix")
    private String titlePrefix;

    @NotBlank
    @Column(name = "name_kr")
    private String nameKr;

    @NotBlank
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

    @Min(0)
    @Column(name = "required_meso_start")
    private Integer requiredMesoStart;

    @Column(name = "start_npc_id")
    private Long startNpcId;

    @Column(name = "end_npc_id")
    private Long endNpcId;
}