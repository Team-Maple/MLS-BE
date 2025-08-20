package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.BookmarkSummaryDto;
import com.maple.api.bookmark.application.dto.ItemBookmarkSearchRequestDto;
import com.maple.api.bookmark.repository.BookmarkQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkQueryService {

    private final BookmarkQueryDslRepository bookmarkQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<BookmarkSummaryDto> getBookmarks(String memberId, Pageable pageable) {
        return bookmarkQueryDslRepository.searchBookmarks(memberId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<BookmarkSummaryDto> getItemBookmarks(String memberId, ItemBookmarkSearchRequestDto request, Pageable pageable) {
        return bookmarkQueryDslRepository.searchItemBookmarks(memberId, request, pageable);
    }
}