package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "북마크 자체 일괄 삭제 응답")
public record BookmarkBulkDeleteResponseDto(
        @Schema(description = "실제로 삭제한 북마크 수", example = "3")
        Integer deletedCount
) {
}
