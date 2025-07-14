package com.maple.api.common.presentation.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {
    String getMessage();
    HttpStatus getStatus();
}