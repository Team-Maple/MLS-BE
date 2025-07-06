package com.maple.api.map.presentation.restapi;

import com.maple.api.map.application.MapService;
import com.maple.api.map.application.dto.MapSearchRequestDto;
import com.maple.api.map.application.dto.MapSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping
    public ResponseEntity<Page<MapSummaryDto>> searchMaps(
            @ParameterObject MapSearchRequestDto request,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MapSummaryDto> maps = mapService.searchMaps(request, pageable);
        return ResponseEntity.ok(maps);
    }
}