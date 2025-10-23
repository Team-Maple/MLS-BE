package com.maple.api.bookmark.application;

import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkFlagService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public Integer findBookmarkId(String memberId, BookmarkType type, Integer resourceId) {
        if (memberId == null) {
            return null;
        }
        return bookmarkRepository.findByMemberIdAndBookmarkTypeAndResourceId(memberId, type, resourceId)
                .map(Bookmark::getBookmarkId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Integer, Integer> findBookmarkIds(String memberId, BookmarkType type, Collection<Integer> resourceIds) {
        if (memberId == null || resourceIds == null || resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<BookmarkRepository.BookmarkProjection> projections = bookmarkRepository.findBookmarkIds(
                memberId,
                type,
                resourceIds.stream().toList()
        );

        return projections.stream()
                .collect(Collectors.toMap(BookmarkRepository.BookmarkProjection::getResourceId,
                        BookmarkRepository.BookmarkProjection::getBookmarkId,
                        (existing, ignored) -> existing));
    }
}
