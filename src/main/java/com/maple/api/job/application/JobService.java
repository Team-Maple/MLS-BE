package com.maple.api.job.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.job.application.dto.JobDto;
import com.maple.api.job.domain.Job;
import com.maple.api.job.exception.JobException;
import com.maple.api.job.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final Map<Integer, Job> jobCache = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> rootJobCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCache() {
        List<Job> jobs = jobRepository.findAll();

        for (Job job : jobs) {
            jobCache.put(job.getJobId(), job);
        }

        for (Job job : jobs) {
            Integer rootJobId = findRootJobId(job.getJobId());
            rootJobCache.put(job.getJobId(), rootJobId);
        }
    }

    public List<JobDto> findAll() {
        return jobCache.values().stream()
                .filter(job -> !job.getJobId().equals(Job.COMMON_JOB_ID))
                .filter(job -> !job.isDisabled())
                .map(JobDto::toDto)
                .toList();
    }

    public JobDto findDetail(Integer jobId) {
        Job job = jobCache.get(jobId);
        if (job == null || job.isDisabled() || Job.COMMON_JOB_ID == job.getJobId()) {
            throw ApiException.of(JobException.JOB_NOT_FOUND);
        }
        return JobDto.toDto(job);
    }

    private Integer findRootJobId(Integer jobId) {
        Job current = jobCache.get(jobId);
        if (current == null) {
            throw ApiException.of(JobException.JOB_NOT_FOUND);
        }

        while (current.getParentJob() != null) {
            current = jobCache.get(current.getParentJob().getJobId());
            if (current == null) {
                throw ApiException.of(JobException.PARENT_JOB_NOT_FOUND);
            }
        }

        return current.getJobId();
    }
}
