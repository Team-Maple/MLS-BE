package com.maple.api.job.application.dto;

import com.maple.api.job.domain.Job;

public record JobDto(
        Integer jobId,
        String jobName,
        Integer jobLevel,
        Integer parentJobId
) {
    public static JobDto toDto(Job job) {
        if (job == null) {
            return null;
        }
        
        return new JobDto(
                job.getJobId(),
                job.getJobName(),
                job.getJobLevel(),
                job.getParentJob() != null ? job.getParentJob().getJobId() : null
        );
    }
}