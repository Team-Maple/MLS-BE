package com.maple.api.quest.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quest_allowed_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestAllowedJob {
    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Column(name = "job_name")
    private String jobName;
}