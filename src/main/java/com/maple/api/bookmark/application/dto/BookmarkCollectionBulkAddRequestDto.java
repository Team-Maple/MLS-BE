package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.lang.Nullable;

import java.util.List;

@Schema(description = "여러 컬렉션에 여러 북마크를 한 번에 추가 요청")
public record BookmarkCollectionBulkAddRequestDto(
        @Schema(description = "북마크를 추가할 컬렉션 ID 리스트", example = "[1, 2, 3]")
        @UniqueElements(message = "중복된 컬렉션 ID가 있습니다.")
        @Nullable
        List<Integer> collectionIds,

        @Schema(description = "추가할 북마크 ID 리스트", example = "[10, 20, 30]")
        @UniqueElements(message = "중복된 북마크 ID가 있습니다.")
        @Nullable
        List<Integer> bookmarkIds
) {
}
