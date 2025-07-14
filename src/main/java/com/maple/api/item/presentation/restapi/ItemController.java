package com.maple.api.item.presentation.restapi;

import com.maple.api.item.application.ItemService;
import com.maple.api.item.application.dto.ItemDetailDto;
import com.maple.api.item.application.dto.ItemSearchRequestDto;
import com.maple.api.item.application.dto.ItemSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<Page<ItemSummaryDto>> searchItems(
            @Valid @ParameterObject ItemSearchRequestDto searchRequest,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Page<ItemSummaryDto> results = itemService.searchItems(searchRequest, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDetailDto> getItemDetail(@PathVariable Integer id) {
        ItemDetailDto itemDetail = itemService.getItemDetail(id);
        return ResponseEntity.ok(itemDetail);
    }
}