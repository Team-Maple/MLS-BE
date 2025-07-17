package com.maple.api.common.presentation.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    
    private final ExceptionCode exceptionCode;

    public ApiException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
    }

    public ApiException(ExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.exceptionCode = exceptionCode;
    }

    public static ApiException of(ExceptionCode exceptionCode) {
        return new ApiException(exceptionCode);
    }

    public static ApiException of(ExceptionCode exceptionCode, Throwable cause) {
        return new ApiException(exceptionCode, cause);
    }
}