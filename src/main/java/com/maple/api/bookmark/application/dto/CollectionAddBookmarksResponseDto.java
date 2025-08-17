package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "컬렉션에 북마크 추가 응답")
public record CollectionAddBookmarksResponseDto(
        @Schema(description = "실제 추가된 북마크 수", example = "2")
        Integer addedCount
) {
    public static CollectionAddBookmarksResponseDto of(Integer addedCount) {
        return new CollectionAddBookmarksResponseDto(addedCount);
    }
}