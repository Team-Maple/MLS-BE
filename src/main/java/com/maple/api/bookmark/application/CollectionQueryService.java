package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.CollectionWithBookmarksDto;
import com.maple.api.bookmark.repository.CollectionQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionQueryService {

    private final CollectionQueryDslRepository collectionQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<CollectionWithBookmarksDto> getCollectionsWithRecentBookmarks(String memberId, Pageable pageable) {
        return collectionQueryDslRepository.findCollectionsWithRecentBookmarks(memberId, pageable);
    }
}