package com.maple.api.global.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException{

    private final ExceptionDetails details;

    public ApiException(ExceptionDetails details) {
        super(details.getMessage());
        this.details = details;
    }

    public static ApiException of(ExceptionDetails code) {
        return new ApiException(code);
    }
}


