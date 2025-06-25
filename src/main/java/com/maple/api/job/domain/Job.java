package com.maple.api.job.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Job {
    @Id
    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "parent_job_id")
    private Integer parentJobId;

    @Column(name = "job_level")
    private Integer jobLevel;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}