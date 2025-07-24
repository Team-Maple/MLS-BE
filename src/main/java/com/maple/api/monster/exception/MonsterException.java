package com.maple.api.monster.exception;

import com.maple.api.common.presentation.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MonsterException implements ExceptionCode {

    MONSTER_NOT_FOUND("몬스터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus status;
}