package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.AddBookmarksToCollectionRequestDto;
import com.maple.api.bookmark.application.dto.AddBookmarksToCollectionResponseDto;
import com.maple.api.bookmark.application.dto.CollectionResponseDto;
import com.maple.api.bookmark.application.dto.CreateCollectionRequestDto;
import com.maple.api.bookmark.domain.Bookmark;
import com.maple.api.bookmark.domain.BookmarkCollection;
import com.maple.api.bookmark.domain.Collection;
import com.maple.api.bookmark.exception.BookmarkException;
import com.maple.api.bookmark.repository.BookmarkCollectionRepository;
import com.maple.api.bookmark.repository.BookmarkRepository;
import com.maple.api.bookmark.repository.CollectionRepository;
import com.maple.api.common.presentation.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkCollectionRepository bookmarkCollectionRepository;

    public CollectionResponseDto createCollection(String memberId, CreateCollectionRequestDto request) {
        Collection collection = new Collection(memberId, request.name());

        return CollectionResponseDto.toDto(collectionRepository.save(collection));
    }

    public AddBookmarksToCollectionResponseDto addBookmarksToCollection(String memberId, Integer collectionId, AddBookmarksToCollectionRequestDto request) {
        // 1. 컬렉션 존재 및 소유권 검증
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ApiException(BookmarkException.COLLECTION_NOT_FOUND));
        
        if (!collection.getMemberId().equals(memberId)) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        // 2. 북마크들의 존재 및 소유권 검증
        List<Bookmark> bookmarks = bookmarkRepository.findAllById(request.bookmarkIds());
        if (bookmarks.size() != request.bookmarkIds().size()) {
            throw new ApiException(BookmarkException.BOOKMARK_NOT_FOUND);
        }
        
        boolean allBookmarksOwnedByUser = bookmarks.stream()
                .allMatch(bookmark -> bookmark.getMemberId().equals(memberId));
        if (!allBookmarksOwnedByUser) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        // 3. 이미 컬렉션에 있는 북마크 체크
        List<Integer> existingBookmarkIds = bookmarkCollectionRepository.findExistingBookmarkIds(collectionId, request.bookmarkIds());
        if (!existingBookmarkIds.isEmpty()) {
            throw new ApiException(BookmarkException.DUPLICATE_BOOKMARK_IN_COLLECTION);
        }

        // 4. sortOrder 계산
        Integer maxSortOrder = bookmarkCollectionRepository.findMaxSortOrderByCollectionId(collectionId).orElse(0);

        // 5. BookmarkCollection 엔티티 생성 및 저장
        List<BookmarkCollection> bookmarkCollections = IntStream.range(0, request.bookmarkIds().size())
                .mapToObj(i -> new BookmarkCollection(
                        request.bookmarkIds().get(i), 
                        collectionId, 
                        maxSortOrder + i + 1))
                .toList();

        bookmarkCollectionRepository.saveAll(bookmarkCollections);

        return AddBookmarksToCollectionResponseDto.of(request.bookmarkIds().size());
    }
}