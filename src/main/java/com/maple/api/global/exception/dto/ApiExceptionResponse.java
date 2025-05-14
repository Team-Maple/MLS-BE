package com.maple.api.global.exception.dto;

import com.maple.api.global.exception.ExceptionDetails;
import com.maple.api.global.exception.details.GlobalExceptionDetails;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ApiExceptionResponse(
        LocalDateTime timestamp,
        String code,
        String message,
        Object details
) {
    private ApiExceptionResponse(String code, String message, Object details) {
        this(LocalDateTime.now(), code, message, details);
    }

    public static ApiExceptionResponse of(ExceptionDetails details) {
        return new ApiExceptionResponse(details.getCode(), details.getMessage(), null);
    }

    public static ApiExceptionResponse of(ExceptionDetails details, Object o) {
        return new ApiExceptionResponse(details.getCode(), details.getMessage(), o);
    }

    public static ApiExceptionResponse of(List<FieldError> fieldErrors) {
        Map<String, String> errors = fieldErrors.stream()
                .collect(Collectors.toMap(FieldError::getField, err -> err.getDefaultMessage() == null ? "null" : err.getDefaultMessage(), (existing, replacement) -> existing));
        return ApiExceptionResponse.of(GlobalExceptionDetails.INVALID_REQUEST_PARAM, errors);
    }
}

