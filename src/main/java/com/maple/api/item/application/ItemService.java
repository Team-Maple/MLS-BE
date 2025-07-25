package com.maple.api.item.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.item.application.dto.ItemDetailDto;
import com.maple.api.item.application.dto.ItemMonsterDropDto;
import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.application.dto.ItemSummaryDto;
import com.maple.api.item.domain.Category;
import com.maple.api.item.domain.EquipmentItem;
import com.maple.api.item.domain.Item;
import com.maple.api.item.domain.ScrollItem;
import com.maple.api.item.exception.ItemException;
import com.maple.api.item.repository.ItemQueryDslRepository;
import com.maple.api.item.repository.ItemRepository;
import com.maple.api.job.domain.Job;
import com.maple.api.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemQueryDslRepository itemQueryDslRepository;
    private final JobRepository jobRepository;

    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public Page<ItemSummaryDto> searchItems(ItemSearchRequestDto searchRequest, Pageable pageable) {
        return itemQueryDslRepository.searchItems(searchRequest, pageable)
                .map(ItemSummaryDto::toDto);
    }

    @Transactional(readOnly = true)
    public ItemDetailDto getItemDetail(Integer itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.of(ItemException.ITEM_NOT_FOUND));

        item = loadTypeSpecificData(item);

        Category leafCategory = categoryService.findById(item.getCategoryId());
        Category rootCategory = categoryService.findRootCategory(item.getCategoryId());

        List<Job> availableJobs = jobRepository.findByItemId(itemId);

        return ItemDetailDto.toDto(item, rootCategory, leafCategory, availableJobs);
    }

    private Item loadTypeSpecificData(Item item) {
        if (item instanceof EquipmentItem) {
            return itemRepository.findEquipmentDetailById(item.getItemId())
                    .orElseThrow(() -> ApiException.of(ItemException.ITEM_NOT_FOUND));
        } else if (item instanceof ScrollItem) {
            return itemRepository.findScrollDetailById(item.getItemId())
                    .orElseThrow(() -> ApiException.of(ItemException.ITEM_NOT_FOUND));
        } else {
            return item;
        }
    }

    @Transactional(readOnly = true)
    public List<ItemMonsterDropDto> getItemDropMonsters(Integer itemId, Sort sort) {
        if (!itemRepository.existsById(itemId)) {
            throw ApiException.of(ItemException.ITEM_NOT_FOUND);
        }

        return itemQueryDslRepository.findItemDropMonstersByItemId(itemId, sort);
    }

}