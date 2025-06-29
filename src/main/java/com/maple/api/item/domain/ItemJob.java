package com.maple.api.item.domain;

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

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "job_id")
    private Integer jobId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}