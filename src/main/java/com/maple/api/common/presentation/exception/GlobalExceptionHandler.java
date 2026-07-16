package com.maple.api.common.presentation.exception;

import com.maple.api.common.logging.SafeExceptionLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                GlobalException.VALIDATION_FAILED,
                request.getRequestURI(),
                errors
        );
        
        httpFailure(log.atWarn(), request, GlobalException.VALIDATION_FAILED.getStatus().value(), ex)
                .log("Request validation failed");
        return ResponseEntity.status(GlobalException.VALIDATION_FAILED.getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ExceptionResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                GlobalException.BIND_FAILED,
                request.getRequestURI(),
                errors
        );
        
        httpFailure(log.atWarn(), request, GlobalException.BIND_FAILED.getStatus().value(), ex)
                .log("Request parameter binding failed");
        return ResponseEntity.status(GlobalException.BIND_FAILED.getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                GlobalException.CONSTRAINT_VIOLATION,
                request.getRequestURI(),
                errors
        );
        
        httpFailure(log.atWarn(), request, GlobalException.CONSTRAINT_VIOLATION.getStatus().value(), ex)
                .log("Request constraint validation failed");
        return ResponseEntity.status(GlobalException.CONSTRAINT_VIOLATION.getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                ex.getMessage(),
                request.getRequestURI()
        );
        
        httpFailure(log.atWarn(), request, GlobalException.INVALID_ARGUMENT.getStatus().value(), ex)
                .log("Request argument was invalid");
        return ResponseEntity.status(GlobalException.INVALID_ARGUMENT.getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                ex.getExceptionCode(),
                request.getRequestURI()
        );
        
        httpFailure(log.atWarn(), request, ex.getExceptionCode().getStatus().value(), ex)
                .log("Request failed with an application error");
        return ResponseEntity.status(ex.getExceptionCode().getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                GlobalException.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
        
        httpFailure(SafeExceptionLog.addException(log.atError(), ex), request,
                GlobalException.INTERNAL_SERVER_ERROR.getStatus().value())
                .log("Unexpected request failure");
        return ResponseEntity.status(GlobalException.INTERNAL_SERVER_ERROR.getStatus()).body(exceptionResponse);
    }

    private LoggingEventBuilder httpFailure(
            LoggingEventBuilder event,
            HttpServletRequest request,
            int status,
            Exception exception) {
        return httpFailure(event, request, status)
                .addKeyValue("error.type", exception.getClass().getName());
    }

    private LoggingEventBuilder httpFailure(
            LoggingEventBuilder event,
            HttpServletRequest request,
            int status) {
        event.addKeyValue("event.action", "http.request.failure")
                .addKeyValue("event.outcome", "failure")
                .addKeyValue("http.request.method", request.getMethod())
                .addKeyValue("http.response.status_code", status);

        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route != null) {
            event.addKeyValue("http.route", route.toString());
        }
        return event;
    }
}
