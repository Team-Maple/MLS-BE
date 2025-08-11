package com.maple.api.bookmark.application;

import com.maple.api.bookmark.application.dto.CollectionResponseDto;
import com.maple.api.bookmark.application.dto.CreateCollectionRequestDto;
import com.maple.api.bookmark.domain.Collection;
import com.maple.api.bookmark.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionResponseDto createCollection(String memberId, CreateCollectionRequestDto request) {
        Collection collection = new Collection(memberId, request.name());

        return CollectionResponseDto.toDto(collectionRepository.save(collection));
    }
}