package com.maple.api.item.repository;

import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemQueryDslRepository {
    Page<Item> searchItems(ItemSearchRequestDto searchRequest, Pageable pageable);
}