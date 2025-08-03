package com.maple.api.quest.application.dto;

import com.maple.api.job.domain.Job;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "퀘스트 허용 직업 DTO")
public record QuestJobDto(
        @Schema(description = "직업 ID", example = "100")
        Integer jobId,
        
        @Schema(description = "직업명", example = "전사")
        String jobName
) {
    public static QuestJobDto toDto(Job job) {
        return new QuestJobDto(
                job.getJobId(),
                job.getJobName()
        );
    }
}