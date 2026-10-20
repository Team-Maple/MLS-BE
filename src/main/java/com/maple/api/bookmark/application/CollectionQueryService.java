package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.bookmark.repository.BookmarkCollectionRepository;
import com.maple.api.bookmark.repository.BookmarkCollectionRepository.CollectionInclusionCount;
import com.maple.api.bookmark.repository.BookmarkRepository;
import com.maple.api.bookmark.repository.CollectionQueryDslRepository;
import com.maple.api.common.presentation.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionQueryService {

    private final CollectionQueryDslRepository collectionQueryDslRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkCollectionRepository bookmarkCollectionRepository;

    @Transactional(readOnly = true)
    public Page<CollectionWithBookmarksDto> getCollectionsWithRecentBookmarks(
            String memberId, Pageable pageable, List<Integer> bookmarkIds) {
        if (bookmarkIds != null) {
            List<Bookmark> bookmarks = bookmarkRepository.findAllById(bookmarkIds);
            if (bookmarks.size() != bookmarkIds.size()) {
                throw new ApiException(BookmarkException.BOOKMARK_NOT_FOUND);
            }
            if (bookmarks.stream().anyMatch(bookmark -> !bookmark.getMemberId().equals(memberId))) {
                throw new ApiException(BookmarkException.ACCESS_DENIED);
            }
        }

        Page<CollectionWithBookmarksDto> collections =
                collectionQueryDslRepository.findCollectionsWithRecentBookmarks(memberId, pageable);
        if (bookmarkIds == null || collections.isEmpty()) {
            return collections;
        }

        List<Integer> collectionIds = collections.stream()
                .map(CollectionWithBookmarksDto::collectionId)
                .toList();
        Map<Integer, Long> includedCounts = bookmarkCollectionRepository
                .countIncludedBookmarksByCollections(memberId, collectionIds, bookmarkIds)
                .stream()
                .collect(Collectors.toMap(
                        CollectionInclusionCount::getCollectionId,
                        CollectionInclusionCount::getIncludedCount
                ));

        return collections.map(collection -> collection.withInclusion(
                Math.toIntExact(includedCounts.getOrDefault(collection.collectionId(), 0L)),
                bookmarkIds.size()
        ));
    }
}
