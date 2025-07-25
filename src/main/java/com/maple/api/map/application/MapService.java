package com.maple.api.map.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.map.application.dto.*;
import com.maple.api.map.exception.MapException;
import com.maple.api.map.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private final MapQueryDslRepository mapQueryDslRepository;
    private final MapRepository mapRepository;
    private final MapMonsterQueryDslRepository mapMonsterQueryDslRepository;
    private final MapNpcQueryDslRepository mapNpcQueryDslRepository;

    @Transactional(readOnly = true)
    public Page<MapSummaryDto> searchMaps(MapSearchRequestDto request, Pageable pageable) {
        return mapQueryDslRepository.searchMaps(request, pageable).map(MapSummaryDto::toDto);
    }

    @Transactional(readOnly = true)
    public MapDetailDto getMapDetail(Integer mapId) {
        com.maple.api.map.domain.Map map = mapRepository.findById(mapId)
                .orElseThrow(() -> ApiException.of(MapException.MAP_NOT_FOUND));

        return MapDetailDto.toDto(map);
    }

    @Transactional(readOnly = true)
    public List<MapMonsterDto> getMapMonsters(Integer mapId, Sort sort) {
        if (!mapRepository.existsById(mapId)) {
            throw ApiException.of(MapException.MAP_NOT_FOUND);
        }

        return mapMonsterQueryDslRepository.findMapMonsterDtosByMapId(mapId, sort);
    }

    @Transactional(readOnly = true)
    public List<MapNpcDto> getMapNpcs(Integer mapId) {
        if (!mapRepository.existsById(mapId)) {
            throw ApiException.of(MapException.MAP_NOT_FOUND);
        }

        return mapNpcQueryDslRepository.findNpcsByMapId(mapId);
    }
}