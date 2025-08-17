package com.maple.api.bookmark.application.dto;

import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "북마크 응답")
public record BookmarkResponseDto(
        @Schema(description = "북마크 ID", example = "1")
        Integer bookmarkId,

        @Schema(description = "북마크 타입", example = "ITEM")
        BookmarkType bookmarkType,

        @Schema(description = "리소스 ID", example = "2070005")
        Integer resourceId
) {
    
    public static BookmarkResponseDto toDto(Bookmark bookmark) {
        return BookmarkResponseDto.builder()
                .bookmarkId(bookmark.getBookmarkId())
                .bookmarkType(bookmark.getBookmarkType())
                .resourceId(bookmark.getResourceId())
                .build();
    }
}