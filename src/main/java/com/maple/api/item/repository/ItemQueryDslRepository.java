package com.maple.api.item.repository;

import com.maple.api.item.application.dto.ItemMonsterDropDto;
import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface ItemQueryDslRepository {
    Page<Item> searchItems(ItemSearchRequestDto searchRequest, Pageable pageable);
    List<ItemMonsterDropDto> findItemDropMonstersByItemId(Integer itemId, Sort sort);
    long countItemsByKeyword(String keyword);
}
