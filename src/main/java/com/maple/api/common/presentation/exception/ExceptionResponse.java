package com.maple.api.common.presentation.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionResponse {
    
    private final String message;
    private final LocalDateTime timestamp;
    private final String path;
    private final Object details;
    
    public static ExceptionResponse of(String message, String path) {
        return ExceptionResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    public static ExceptionResponse of(String message, String path, Object details) {
        return ExceptionResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .details(details)
                .build();
    }
    
    public static ExceptionResponse of(ExceptionCode exceptionCode, String path) {
        return ExceptionResponse.builder()
                .message(exceptionCode.getMessage())
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    public static ExceptionResponse of(ExceptionCode exceptionCode, String path, Object details) {
        return ExceptionResponse.builder()
                .message(exceptionCode.getMessage())
                .timestamp(LocalDateTime.now())
                .path(path)
                .details(details)
                .build();
    }
}