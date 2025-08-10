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
        if (bookmarkRepository.existsByMemberIdAndBookmarkTypeAndResourceId(memberId, request.getBookmarkType(), request.getResourceId())) {
            throw new ApiException(BookmarkException.DUPLICATE_BOOKMARK);
        }

        Bookmark bookmark = new Bookmark(memberId, request.getBookmarkType(), request.getResourceId());

        return BookmarkResponseDto.toDto(bookmarkRepository.save(bookmark));
    }
}