package com.maple.api.bookmark.repository;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookmarkQueryDslRepository {
    Page<BookmarkSummaryDto> searchBookmarks(String memberId, Pageable pageable);
}