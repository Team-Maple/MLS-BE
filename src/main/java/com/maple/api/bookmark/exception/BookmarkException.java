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
    COLLECTION_NOT_FOUND("컬렉션을 찾을 수 없습니다.", NOT_FOUND),
    ACCESS_DENIED("해당 리소스에 접근할 권한이 없습니다.", FORBIDDEN),
    DUPLICATE_BOOKMARK_IN_COLLECTION("이미 컬렉션에 포함된 북마크입니다.", CONFLICT),
    ;

    private final String message;
    private final HttpStatus status;
}