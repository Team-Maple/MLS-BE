package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여러 컬렉션에 여러 북마크를 한 번에 추가 응답")
public record BookmarkCollectionBulkAddResponseDto(
        @Schema(description = "실제 추가된 북마크-컬렉션 관계 수", example = "6")
        Integer addedCount
) {
    public static BookmarkCollectionBulkAddResponseDto of(Integer addedCount) {
        return new BookmarkCollectionBulkAddResponseDto(addedCount);
    }
}
