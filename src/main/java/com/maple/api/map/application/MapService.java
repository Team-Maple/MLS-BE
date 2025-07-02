package com.maple.api.map.application;

import com.maple.api.map.application.dto.MapSearchRequestDto;
import com.maple.api.map.application.dto.MapSummaryDto;
import com.maple.api.map.repository.MapQueryDslRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MapService {

    private final MapQueryDslRepository mapQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<MapSummaryDto> searchMaps(MapSearchRequestDto request, Pageable pageable) {
        return mapQueryDslRepository.searchMaps(request, pageable).map(MapSummaryDto::toDto);
    }
}