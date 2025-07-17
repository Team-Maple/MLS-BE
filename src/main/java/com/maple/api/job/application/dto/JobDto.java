package com.maple.api.job.application.dto;

import com.maple.api.job.domain.Job;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업 정보")
public record JobDto(
        @Schema(description = "직업 ID", example = "1")
        Integer jobId,
        
        @Schema(description = "직업 이름", example = "초보자")
        String jobName,
        
        @Schema(description = "직업 레벨 (전직 단계)", example = "0")
        Integer jobLevel,
        
        @Schema(description = "상위 직업 ID (전직 전 직업)", example = "null")
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