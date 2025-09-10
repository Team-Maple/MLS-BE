package com.maple.api.bookmark.application;

import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookmarkFlagService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public boolean isBookmarked(String memberId, BookmarkType type, Integer resourceId) {
        if (memberId == null) return false;
        return bookmarkRepository.existsByMemberIdAndBookmarkTypeAndResourceId(memberId, type, resourceId);
    }

    @Transactional(readOnly = true)
    public Set<Integer> findBookmarkedIds(String memberId, BookmarkType type, Collection<Integer> resourceIds) {
        if (memberId == null || resourceIds == null || resourceIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Integer> list = bookmarkRepository.findBookmarkedResourceIds(memberId, type, resourceIds.stream().toList());
        return new HashSet<>(list);
    }
}

