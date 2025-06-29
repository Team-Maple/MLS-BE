package com.maple.api.search.repository;

import com.maple.api.search.domain.VwSearchSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VwSearchSummaryRepository extends JpaRepository<VwSearchSummary, String> {
}