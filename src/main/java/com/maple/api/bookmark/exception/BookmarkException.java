package com.maple.api.bookmark.exception;

import com.maple.api.common.presentation.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum BookmarkException implements ExceptionCode {

    DUPLICATE_BOOKMARK("이미 북마크된 리소스입니다.", CONFLICT),
    BOOKMARK_NOT_FOUND("북마크를 찾을 수 없습니다.", NOT_FOUND),
    ;

    private final String message;
    private final HttpStatus status;
}