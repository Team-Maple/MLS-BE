package com.maple.api.item.domain;

import com.maple.api.job.domain.Job;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "items_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private EquipmentItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}