package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "컬렉션-북마크 연결 일괄 제거 응답")
public record BookmarkCollectionBulkRemoveResponseDto(
        @Schema(description = "실제로 제거한 북마크-컬렉션 연결 수", example = "6")
        Integer removedCount
) {
}
