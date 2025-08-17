package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "북마크를 여러 컬렉션에 추가 응답")
public record BookmarkAddToCollectionsResponseDto(
        @Schema(description = "실제 추가된 컬렉션 수", example = "3")
        Integer addedCount
) {
    public static BookmarkAddToCollectionsResponseDto of(Integer addedCount) {
        return new BookmarkAddToCollectionsResponseDto(addedCount);
    }
}