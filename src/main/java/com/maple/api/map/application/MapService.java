package com.maple.api.map.application;

import com.maple.api.bookmark.application.BookmarkFlagService;
import com.maple.api.bookmark.domain.BookmarkType;
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
    private final BookmarkFlagService bookmarkFlagService;

    @Transactional(readOnly = true)
    public Page<MapSummaryDto> searchMaps(String memberId, MapSearchRequestDto request, Pageable pageable) {
        var page = mapQueryDslRepository.searchMaps(request, pageable);
        var ids = page.getContent().stream().map(com.maple.api.map.domain.Map::getMapId).toList();
        var bookmarkIds = bookmarkFlagService.findBookmarkIds(memberId, BookmarkType.MAP, ids);
        return page.map(m -> MapSummaryDto.toDto(m, bookmarkIds.get(m.getMapId())));
    }

    @Transactional(readOnly = true)
    public MapDetailDto getMapDetail(String memberId, Integer mapId) {
        com.maple.api.map.domain.Map map = mapRepository.findById(mapId)
                .orElseThrow(() -> ApiException.of(MapException.MAP_NOT_FOUND));

        Integer bookmarkId = bookmarkFlagService.findBookmarkId(memberId, BookmarkType.MAP, mapId);
        return MapDetailDto.toDto(map, bookmarkId);
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
