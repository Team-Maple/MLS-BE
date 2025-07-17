package com.maple.api.job.presentation.restapi;

import com.maple.api.job.application.JobService;
import com.maple.api.job.application.dto.JobDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Job", description = "직업 관련 API")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(
        summary = "모든 직업 조회",
        description = "메이플스토리에서 사용 가능한 모든 직업 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "직업 목록 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<JobDto>> findAllJobs() {
        return ResponseEntity.ok(jobService.findAll());
    }
}