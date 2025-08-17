package com.maple.api.bookmark.application.dto;

import com.maple.api.bookmark.domain.BookmarkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "북마크 추가 요청")
public record CreateBookmarkRequestDto(
        @NotNull(message = "북마크 타입은 필수입니다.")
        @Schema(description = "북마크 타입", example = "ITEM", allowableValues = {"ITEM", "MONSTER", "NPC", "QUEST", "MAP"})
        BookmarkType bookmarkType,

        @NotNull(message = "리소스 ID는 필수입니다.")
        @Schema(description = "북마크할 리소스 ID", example = "2070005")
        Integer resourceId
) {}