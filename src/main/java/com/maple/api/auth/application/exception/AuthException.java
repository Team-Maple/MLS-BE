package com.maple.api.auth.application.exception;

import com.maple.api.common.presentation.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum AuthException implements ExceptionCode {

  NO_MEMBER("멤버 정보를 찾을 수 없습니다.", CONFLICT),
  ;

  private final String message;
  private final HttpStatus status;
}