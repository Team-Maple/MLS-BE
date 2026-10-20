package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Schema(description = "북마크 자체 일괄 삭제 요청")
public record BookmarkBulkDeleteRequestDto(
        @NotEmpty(message = "최소 1개 이상의 북마크 ID가 필요합니다.")
        @UniqueElements(message = "중복된 북마크 ID가 있습니다.")
        @Schema(description = "삭제할 북마크 ID 목록", example = "[101, 102, 103]")
        List<@NotNull @Positive Integer> bookmarkIds
) {
}
