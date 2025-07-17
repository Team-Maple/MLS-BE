package com.maple.api.item.exception;

import com.maple.api.common.presentation.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum ItemException implements ExceptionCode {

    // 아이템 관련 예외
    ITEM_NOT_FOUND("아이템을 찾을 수 없습니다.", NOT_FOUND),
    CATEGORY_NOT_FOUND("카테고리를 찾을 수 없습니다.", NOT_FOUND),
    PARENT_CATEGORY_NOT_FOUND("상위 카테고리를 찾을 수 없습니다.", NOT_FOUND);

    private final String message;
    private final HttpStatus status;
}