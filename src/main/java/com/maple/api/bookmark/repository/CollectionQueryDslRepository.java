package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CollectionQueryDslRepository {
    Page<CollectionWithBookmarksDto> findCollectionsWithRecentBookmarks(String memberId, Pageable pageable);
}