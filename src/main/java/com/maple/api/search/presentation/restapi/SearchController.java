package com.maple.api.search.presentation.restapi;

import com.maple.api.search.application.SearchService;
import com.maple.api.search.application.dto.SearchSummaryDto;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<Page<SearchSummaryDto>> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<SearchSummaryDto> results = searchService.search(keyword, pageable);
        return ResponseEntity.ok(results);
    }
}