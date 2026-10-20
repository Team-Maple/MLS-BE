package com.maple.api.bookmark.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.maple.api.bookmark.domain.Collection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "북마크를 포함한 컬렉션 정보")
public record CollectionWithBookmarksDto(
        @Schema(description = "컬렉션 ID", example = "1")
        Integer collectionId,

        @Schema(description = "컬렉션 이름", example = "내가 좋아하는 장비")
        String name,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "최신 북마크 목록 (최대 4개)")
        List<BookmarkSummaryDto> recentBookmarks,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "선택한 북마크 중 이 컬렉션에 포함된 개수. bookmarkIds를 지정한 경우에만 반환",
                example = "2")
        Integer includedCount,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "선택한 북마크가 모두 포함되어 있는지 여부. bookmarkIds를 지정한 경우에만 반환",
                example = "false")
        Boolean allIncluded
) {
    
    public static CollectionWithBookmarksDto of(Collection collection, List<BookmarkSummaryDto> bookmarks) {
        return new CollectionWithBookmarksDto(
                collection.getCollectionId(),
                collection.getName(),
                collection.getCreatedAt(),
                bookmarks,
                null,
                null
        );
    }

    public CollectionWithBookmarksDto withInclusion(int includedCount, int selectedCount) {
        return new CollectionWithBookmarksDto(
                collectionId, name, createdAt, recentBookmarks,
                includedCount, includedCount == selectedCount
        );
    }
}
