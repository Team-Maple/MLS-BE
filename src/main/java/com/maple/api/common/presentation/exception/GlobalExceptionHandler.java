package com.maple.api.common.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        
        log.warn("Validation failed: {}", errors);
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
        
        log.warn("Bind failed: {}", errors);
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
        
        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.status(GlobalException.CONSTRAINT_VIOLATION.getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(GlobalException.INVALID_ARGUMENT.getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                ex.getExceptionCode(),
                request.getRequestURI()
        );
        
        log.warn("API exception occurred: {}", ex.getMessage());
        return ResponseEntity.status(ex.getExceptionCode().getStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        ExceptionResponse exceptionResponse = ExceptionResponse.of(
                GlobalException.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
        
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(GlobalException.INTERNAL_SERVER_ERROR.getStatus()).body(exceptionResponse);
    }
}