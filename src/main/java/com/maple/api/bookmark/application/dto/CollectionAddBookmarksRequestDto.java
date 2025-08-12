package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Schema(description = "컬렉션에 북마크 추가 요청")
public record CollectionAddBookmarksRequestDto(
        @NotNull(message = "북마크 ID 리스트는 필수입니다.")
        @NotEmpty(message = "최소 1개 이상의 북마크 ID가 필요합니다.")
        @UniqueElements(message = "중복된 북마크 ID가 있습니다.")
        @Schema(description = "추가할 북마크 ID 리스트", example = "[1, 2, 3]")
        List<Integer> bookmarkIds
) {}