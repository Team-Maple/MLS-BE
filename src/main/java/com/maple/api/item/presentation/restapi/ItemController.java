package com.maple.api.item.presentation.restapi;

import com.maple.api.common.presentation.restapi.ResponseTemplate;
import com.maple.api.item.application.ItemService;
import com.maple.api.item.application.dto.ItemSearchRequest;
import com.maple.api.item.application.dto.ItemSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<ResponseTemplate<Page<ItemSummaryDto>>> searchItems(
            @Valid @ModelAttribute ItemSearchRequest searchRequest,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Page<ItemSummaryDto> results = itemService.searchItems(searchRequest, pageable);
        return ResponseEntity.ok(ResponseTemplate.success(results));
    }
}