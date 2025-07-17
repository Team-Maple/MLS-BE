package com.maple.api.common.presentation.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum GlobalException implements ExceptionCode {

    // 검증 및 파라미터 예외
    VALIDATION_FAILED("입력값 검증에 실패했습니다.", BAD_REQUEST),
    BIND_FAILED("요청 파라미터 바인딩에 실패했습니다.", BAD_REQUEST),
    CONSTRAINT_VIOLATION("제약 조건 위반입니다.", BAD_REQUEST),
    INVALID_ARGUMENT("잘못된 인수입니다.", BAD_REQUEST),
    
    // 404
    RESOURCE_NOT_FOUND("리소스가 존재하지 않습니다.", NOT_FOUND),
    
    // 서버 예외
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus status;
}