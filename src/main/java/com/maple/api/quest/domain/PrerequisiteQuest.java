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
@Table(name = "quest_prerequisite_quests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PrerequisiteQuest extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "required_to_start_quest_id")
    private Integer requiredToStartQuestId;

    @Column(name = "state")
    private Integer state;
}
