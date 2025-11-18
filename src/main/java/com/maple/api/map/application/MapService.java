package com.maple.api.map.application;

import com.maple.api.bookmark.application.BookmarkFlagService;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.job.exception.JobException;
import com.maple.api.job.repository.JobRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MapService {


    private final MapQueryDslRepository mapQueryDslRepository;
    private final MapRepository mapRepository;
    private final MapMonsterQueryDslRepository mapMonsterQueryDslRepository;
    private final MapNpcQueryDslRepository mapNpcQueryDslRepository;
    private final BookmarkFlagService bookmarkFlagService;
    private final Optional<MapRecommendationRepository> mapRecommendationRepository;
    private final JobRepository jobRepository;

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

    @Transactional(readOnly = true)
    public long countMapsByKeyword(String keyword) {
        return mapQueryDslRepository.countMapsByKeyword(keyword);
    }

    @Transactional(readOnly = true)
    public List<MapRecommendationDto> recommendMaps(int level, int jobId, Integer limit) {
        validateJobExists(jobId);

        MapRecommendationRepository repository = mapRecommendationRepository
                .orElseThrow(() -> ApiException.of(MapException.MAP_RECOMMENDATION_UNAVAILABLE));

        int sanitizedLimit = limit == null || limit <= 0 ? 5 : limit;

        return repository.findRecommendedMaps(level, jobId, sanitizedLimit);
    }

    private void validateJobExists(int jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw ApiException.of(JobException.JOB_NOT_FOUND);
        }
    }
}
