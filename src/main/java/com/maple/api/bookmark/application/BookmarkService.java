package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.BookmarkResponseDto;
import com.maple.api.bookmark.application.dto.CreateBookmarkRequestDto;
import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.bookmark.repository.BookmarkRepository;
import com.maple.api.common.presentation.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    public BookmarkResponseDto createBookmark(String memberId, CreateBookmarkRequestDto request) {
        if (bookmarkRepository.existsByMemberIdAndBookmarkTypeAndResourceId(memberId, request.bookmarkType(), request.resourceId())) {
            throw new ApiException(BookmarkException.DUPLICATE_BOOKMARK);
        }

        Bookmark bookmark = new Bookmark(memberId, request.bookmarkType(), request.resourceId());

        return BookmarkResponseDto.toDto(bookmarkRepository.save(bookmark));
    }

    public void deleteBookmark(String memberId, Integer bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ApiException(BookmarkException.BOOKMARK_NOT_FOUND));
        
        if (!bookmark.getMemberId().equals(memberId)) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        bookmarkRepository.delete(bookmark);
    }
}