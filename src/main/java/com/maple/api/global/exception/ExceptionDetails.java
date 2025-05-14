package com.maple.api.global.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionDetails {
    HttpStatus getStatus();
    String getMessage();
    String getCode();
}
