package com.maple.api.map.repository;

import com.maple.api.map.application.dto.MapSearchRequestDto;
import com.maple.api.map.domain.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MapQueryDslRepository {
    Page<Map> searchMaps(MapSearchRequestDto request, Pageable pageable);
}