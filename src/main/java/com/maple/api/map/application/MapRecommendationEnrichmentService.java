package com.maple.api.map.application;

import com.maple.api.bookmark.application.BookmarkFlagService;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.map.application.dto.MapRecommendationResultDto;
import com.maple.api.map.domain.Map;
import com.maple.api.map.domain.RecommendationCandidate;
import com.maple.api.map.repository.MapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapRecommendationEnrichmentService {

    private final MapRepository mapRepository;
    private final BookmarkFlagService bookmarkFlagService;

    public List<MapRecommendationResultDto> enrich(
            String memberId,
            List<RecommendationCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Integer> mapIds = candidates.stream()
                .map(RecommendationCandidate::mapId)
                .map(Math::toIntExact)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));

        var mapsById = mapRepository.findByMapIdIn(mapIds).stream()
                .collect(Collectors.toMap(Map::getMapId, Function.identity()));

        var bookmarkIds = bookmarkFlagService.findBookmarkIds(memberId, BookmarkType.MAP, mapIds);

        return candidates.stream()
                .map(candidate -> {
                    int mapId = Math.toIntExact(candidate.mapId());
                    Map map = mapsById.get(mapId);
                    return new MapRecommendationResultDto(
                            mapId,
                            candidate.score(),
                            map != null ? map.getIconUrl() : null,
                            map != null ? map.getNameKr() : null,
                            bookmarkIds.get(mapId),
                            candidate.reasons()
                    );
                })
                .toList();
    }
}
