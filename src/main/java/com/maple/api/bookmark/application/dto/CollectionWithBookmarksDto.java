package com.maple.api.bookmark.application.dto;

import com.maple.api.bookmark.domain.Collection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "북마크를 포함한 컬렉션 정보")
public record CollectionWithBookmarksDto(
        @Schema(description = "컬렉션 ID", example = "1")
        Integer collectionId,

        @Schema(description = "컬렉션 이름", example = "내가 좋아하는 장비")
        String name,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "최신 북마크 목록 (최대 4개)")
        List<BookmarkSummaryDto> recentBookmarks
) {
    
    public static CollectionWithBookmarksDto of(Collection collection, List<BookmarkSummaryDto> bookmarks) {
        return new CollectionWithBookmarksDto(
                collection.getCollectionId(),
                collection.getName(),
                collection.getCreatedAt(),
                bookmarks
        );
    }
}