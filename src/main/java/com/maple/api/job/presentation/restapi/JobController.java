package com.maple.api.job.presentation.restapi;

import com.maple.api.job.application.JobService;
import com.maple.api.job.application.dto.JobDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<List<JobDto>> findAllJobs() {
        return ResponseEntity.ok(jobService.findAll());
    }
}