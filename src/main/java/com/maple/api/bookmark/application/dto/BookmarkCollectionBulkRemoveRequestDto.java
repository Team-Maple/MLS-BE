package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Schema(description = "여러 컬렉션에서 선택한 북마크 연결 제거 요청")
public record BookmarkCollectionBulkRemoveRequestDto(
        @NotEmpty(message = "최소 1개 이상의 컬렉션 ID가 필요합니다.")
        @UniqueElements(message = "중복된 컬렉션 ID가 있습니다.")
        @Schema(description = "북마크 연결을 제거할 컬렉션 ID 목록", example = "[1, 2]")
        List<@NotNull @Positive Integer> collectionIds,

        @NotEmpty(message = "최소 1개 이상의 북마크 ID가 필요합니다.")
        @UniqueElements(message = "중복된 북마크 ID가 있습니다.")
        @Schema(description = "각 컬렉션에서 제거할 북마크 ID 목록", example = "[101, 102, 103]")
        List<@NotNull @Positive Integer> bookmarkIds
) {
}
