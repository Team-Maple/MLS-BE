package com.maple.api.global.exception;

import com.maple.api.global.exception.details.GlobalExceptionDetails;
import com.maple.api.global.exception.dto.ApiExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiExceptionResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return ApiExceptionResponse.of(ex.getFieldErrors());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiExceptionResponse missingServletRequestParameterException() {
        return ApiExceptionResponse.of(GlobalExceptionDetails.INVALID_REQUEST_PARAM);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiExceptionResponse> handleBusinessException(ApiException exception) {
        ExceptionDetails exceptionDetails = exception.getDetails();
        return ResponseEntity.status(exceptionDetails.getStatus())
                .body(ApiExceptionResponse.of(exceptionDetails));
    }

    @ExceptionHandler(Exception.class)
    public ApiExceptionResponse handleBusinessException(Exception e) {
        log.error(e.getMessage(), e);
        return ApiExceptionResponse.of(GlobalExceptionDetails.INTERNAL_SERVER_ERROR);
    }
}
