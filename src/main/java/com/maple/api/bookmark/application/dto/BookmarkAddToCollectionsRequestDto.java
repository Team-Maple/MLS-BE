package com.maple.api.bookmark.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Schema(description = "북마크를 여러 컬렉션에 추가 요청")
public record BookmarkAddToCollectionsRequestDto(
        @NotEmpty(message = "최소 1개 이상의 컬렉션 ID가 필요합니다.")
        @UniqueElements(message = "중복된 컬렉션 ID가 있습니다.")
        @Schema(description = "북마크를 추가할 컬렉션 ID 리스트", example = "[1, 2, 3]")
        List<Integer> collectionIds
) {}