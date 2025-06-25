package com.maple.api.quest.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "quest_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuestReward {
    @Id
    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "exp")
    private Long exp;

    @Column(name = "meso")
    private Long meso;

    @Column(name = "popularity")
    private Integer popularity;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}