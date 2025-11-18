package com.maple.api.map.exception;

import com.maple.api.common.presentation.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum MapException implements ExceptionCode {

    MAP_NOT_FOUND("존재하지 않는 맵입니다.", NOT_FOUND),
    MAP_RECOMMENDATION_UNAVAILABLE("사냥터 추천 기능을 사용할 수 없습니다.", SERVICE_UNAVAILABLE);

    private final String message;
    private final HttpStatus status;
}
