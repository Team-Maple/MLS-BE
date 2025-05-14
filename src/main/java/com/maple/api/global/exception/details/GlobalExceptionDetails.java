package com.maple.api.global.exception.details;

import com.maple.api.global.exception.ExceptionDetails;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalExceptionDetails implements ExceptionDetails {

    GLOBAL_NOT_FOUND("리소스가 존재하지 않습니다.", HttpStatus.NOT_FOUND, "G_001"),
    INVALID_REQUEST_PARAM("요청 파라미터가 유효하지 않습니다.", HttpStatus.BAD_REQUEST, "G_002"),
    INTERNAL_SERVER_ERROR("서버에서 발생한 문제입니다. 담당자에게 문의바랍니다.", HttpStatus.INTERNAL_SERVER_ERROR, "G_003")
    ;

    private final String message;
    private final HttpStatus status;
    private final String code;
}

