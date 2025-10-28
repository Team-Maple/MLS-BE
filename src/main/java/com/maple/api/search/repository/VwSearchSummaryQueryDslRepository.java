package com.maple.api.search.repository;

import com.maple.api.search.domain.VwSearchSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VwSearchSummaryQueryDslRepository {
    Page<VwSearchSummary> search(String keyword, Pageable pageable);
    long countByKeyword(String keyword);
}
