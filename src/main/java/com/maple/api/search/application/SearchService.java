package com.maple.api.search.application;

import com.maple.api.search.application.dto.SearchSummaryDto;
import com.maple.api.search.domain.VwSearchSummary;
import com.maple.api.search.repository.VwSearchSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final VwSearchSummaryRepository vwSearchSummaryRepository;

    public Page<SearchSummaryDto> search(String keyword, Pageable pageable) {
        Page<VwSearchSummary> resultPage = vwSearchSummaryRepository.search(keyword, pageable);
        return resultPage.map(SearchSummaryDto::toDto);
    }

}