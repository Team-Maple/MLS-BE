package com.maple.api.item.repository;

import com.maple.api.item.application.dto.ItemSearchRequest;
import com.maple.api.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemQueryDslRepository {
    Page<Item> searchItems(ItemSearchRequest searchRequest, Pageable pageable);
}