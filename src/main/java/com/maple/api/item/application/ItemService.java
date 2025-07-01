package com.maple.api.item.application;

import com.maple.api.item.application.dto.ItemSearchRequest;
import com.maple.api.item.application.dto.ItemSummaryDto;
import com.maple.api.item.repository.ItemQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemQueryDslRepository itemQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<ItemSummaryDto> searchItems(ItemSearchRequest searchRequest, Pageable pageable) {
        return itemQueryDslRepository.searchItems(searchRequest, pageable)
                .map(ItemSummaryDto::toDto);
    }
}