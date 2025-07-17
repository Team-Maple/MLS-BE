package com.maple.api.job.exception;

import com.maple.api.common.presentation.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum JobException implements ExceptionCode {

    // 직업 관련 예외
    JOB_NOT_FOUND("직업을 찾을 수 없습니다.", NOT_FOUND),
    PARENT_JOB_NOT_FOUND("상위 직업을 찾을 수 없습니다.", NOT_FOUND);

    private final String message;
    private final HttpStatus status;
}