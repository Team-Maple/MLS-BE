package com.maple.api.quest.domain;

import com.maple.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quest_allowed_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestAllowedJob extends BaseEntity {
    @Id
    private Integer id;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "job_id")
    private Integer jobId;
}