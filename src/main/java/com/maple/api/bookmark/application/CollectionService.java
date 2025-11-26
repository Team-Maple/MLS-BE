package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.BookmarkAddToCollectionsRequestDto;
import com.maple.api.bookmark.application.dto.BookmarkAddToCollectionsResponseDto;
import com.maple.api.bookmark.application.dto.CollectionAddBookmarksRequestDto;
import com.maple.api.bookmark.application.dto.CollectionAddBookmarksResponseDto;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkAddRequestDto;
import com.maple.api.bookmark.application.dto.BookmarkCollectionBulkAddResponseDto;
import com.maple.api.bookmark.application.dto.CollectionResponseDto;
import com.maple.api.bookmark.application.dto.CreateCollectionRequestDto;
import com.maple.api.bookmark.application.dto.UpdateCollectionRequestDto;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public CollectionAddBookmarksResponseDto addBookmarksToCollection(String memberId, Integer collectionId, CollectionAddBookmarksRequestDto request) {
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

        // 4. BookmarkCollection 엔티티 생성 및 저장
        List<BookmarkCollection> bookmarkCollections = request.bookmarkIds().stream()
                .map(bookmarkId -> new BookmarkCollection(bookmarkId, collectionId))
                .toList();

        bookmarkCollectionRepository.saveAll(bookmarkCollections);

        return CollectionAddBookmarksResponseDto.of(request.bookmarkIds().size());
    }

    public BookmarkAddToCollectionsResponseDto addBookmarkToCollections(String memberId, Integer bookmarkId, BookmarkAddToCollectionsRequestDto request) {
        // 1. 북마크 존재 및 소유권 검증
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ApiException(BookmarkException.BOOKMARK_NOT_FOUND));
        
        if (!bookmark.getMemberId().equals(memberId)) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        // 2. 컬렉션들의 존재 및 소유권 검증
        List<Collection> collections = collectionRepository.findAllById(request.collectionIds());
        if (collections.size() != request.collectionIds().size()) {
            throw new ApiException(BookmarkException.COLLECTION_NOT_FOUND);
        }
        
        boolean allCollectionsOwnedByUser = collections.stream()
                .allMatch(collection -> collection.getMemberId().equals(memberId));
        if (!allCollectionsOwnedByUser) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        // 3. 이미 북마크가 있는 컬렉션 체크
        List<Integer> existingCollectionIds = bookmarkCollectionRepository
                .findExistingCollectionIds(bookmarkId, request.collectionIds());
        if (!existingCollectionIds.isEmpty()) {
            throw new ApiException(BookmarkException.DUPLICATE_BOOKMARK_IN_COLLECTION);
        }

        // 4. BookmarkCollection 엔티티 생성
        List<BookmarkCollection> bookmarkCollections = request.collectionIds().stream()
                .map(collectionId -> new BookmarkCollection(bookmarkId, collectionId))
                .toList();

        // 5. 저장
        bookmarkCollectionRepository.saveAll(bookmarkCollections);

        return BookmarkAddToCollectionsResponseDto.of(request.collectionIds().size());
    }

    public BookmarkCollectionBulkAddResponseDto addBookmarksToCollections(String memberId, BookmarkCollectionBulkAddRequestDto request) {
        // 1. 컬렉션 존재 및 소유권 검증
        List<Integer> collectionIds = request.collectionIds();
        List<Integer> bookmarkIds = request.bookmarkIds();

        List<Collection> collections = collectionRepository.findAllById(collectionIds);
        if (collections.size() != collectionIds.size()) {
            throw new ApiException(BookmarkException.COLLECTION_NOT_FOUND);
        }

        boolean allCollectionsOwnedByUser = collections.stream()
                .allMatch(collection -> collection.getMemberId().equals(memberId));
        if (!allCollectionsOwnedByUser) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        // 2. 북마크 존재 및 소유권 검증
        List<Bookmark> bookmarks = bookmarkRepository.findAllById(bookmarkIds);
        if (bookmarks.size() != bookmarkIds.size()) {
            throw new ApiException(BookmarkException.BOOKMARK_NOT_FOUND);
        }

        boolean allBookmarksOwnedByUser = bookmarks.stream()
                .allMatch(bookmark -> bookmark.getMemberId().equals(memberId));
        if (!allBookmarksOwnedByUser) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        // 3. 기존 매핑 조회 후 중복 제거
        List<BookmarkCollection> existingMappings = bookmarkCollectionRepository
                .findByCollectionIdInAndBookmarkIdIn(collectionIds, bookmarkIds);
        Set<String> existingPairs = new HashSet<>();
        existingMappings.forEach(mapping -> existingPairs.add(mapping.getCollectionId() + "-" + mapping.getBookmarkId()));

        // 4. BookmarkCollection 엔티티 생성 및 저장 (중복 제외)
        List<BookmarkCollection> bookmarkCollections = collectionIds.stream()
                .flatMap(collectionId -> bookmarkIds.stream()
                        .filter(bookmarkId -> !existingPairs.contains(collectionId + "-" + bookmarkId))
                        .map(bookmarkId -> new BookmarkCollection(bookmarkId, collectionId)))
                .toList();

        if (!bookmarkCollections.isEmpty()) {
            bookmarkCollectionRepository.saveAll(bookmarkCollections);
        }

        return BookmarkCollectionBulkAddResponseDto.of(bookmarkCollections.size());
    }

    public void validateCollectionOwnership(String memberId, Integer collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ApiException(BookmarkException.COLLECTION_NOT_FOUND));
        
        if (!collection.getMemberId().equals(memberId)) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }
    }

    public void deleteCollection(String memberId, Integer collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ApiException(BookmarkException.COLLECTION_NOT_FOUND));
        
        if (!collection.getMemberId().equals(memberId)) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        bookmarkCollectionRepository.deleteByCollectionId(collectionId);
        collectionRepository.delete(collection);
    }

    public CollectionResponseDto updateCollectionName(String memberId, Integer collectionId, UpdateCollectionRequestDto request) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ApiException(BookmarkException.COLLECTION_NOT_FOUND));

        if (!collection.getMemberId().equals(memberId)) {
            throw new ApiException(BookmarkException.ACCESS_DENIED);
        }

        collection.rename(request.name());

        return CollectionResponseDto.toDto(collection);
    }
}
