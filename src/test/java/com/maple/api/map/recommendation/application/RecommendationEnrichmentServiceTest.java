package com.maple.api.map.recommendation.application;

import com.maple.api.bookmark.application.BookmarkFlagService;
import com.maple.api.bookmark.domain.BookmarkType;
import com.maple.api.map.domain.Map;
import com.maple.api.map.recommendation.domain.RecommendationCandidate;
import com.maple.api.map.repository.MapRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationEnrichmentServiceTest {

    private final MapRepository mapRepository = mock(MapRepository.class);
    private final BookmarkFlagService bookmarkFlagService = mock(BookmarkFlagService.class);
    private final RecommendationEnrichmentService service =
            new RecommendationEnrichmentService(mapRepository, bookmarkFlagService);

    @Test
    void bulkLoadsMapsAndBookmarksOnceAndPreservesScoreOrder() {
        List<RecommendationCandidate> candidates = List.of(
                new RecommendationCandidate(200L, 2.0d, List.of()),
                new RecommendationCandidate(100L, 1.0d, List.of())
        );
        when(mapRepository.findByMapIdIn(List.of(200, 100))).thenReturn(List.of(
                map(100, "low"),
                map(200, "high")
        ));
        when(bookmarkFlagService.findBookmarkIds("member", BookmarkType.MAP, List.of(200, 100)))
                .thenReturn(java.util.Map.of(100, 10));

        var result = service.enrich("member", candidates);

        assertThat(result).extracting(EnrichedRecommendationCandidate::mapId)
                .containsExactly(200, 100);
        assertThat(result).extracting(EnrichedRecommendationCandidate::bookmarkId)
                .containsExactly(null, 10);
        verify(mapRepository).findByMapIdIn(List.of(200, 100));
        verify(bookmarkFlagService).findBookmarkIds("member", BookmarkType.MAP, List.of(200, 100));
    }

    @Test
    void emptyCandidatesSkipAllEnrichmentQueries() {
        assertThat(service.enrich("member", List.of())).isEmpty();
        verifyNoInteractions(mapRepository, bookmarkFlagService);
    }

    @Test
    void auraCandidateWithoutCanonicalMapKeepsLegacyNullableEnrichmentContract() {
        List<RecommendationCandidate> candidates = List.of(
                new RecommendationCandidate(100L, 1.0d, List.of())
        );
        when(mapRepository.findByMapIdIn(List.of(100))).thenReturn(List.of());
        when(bookmarkFlagService.findBookmarkIds("member", BookmarkType.MAP, List.of(100)))
                .thenReturn(java.util.Map.of(100, 17));

        assertThat(service.enrich("member", candidates)).singleElement().satisfies(candidate -> {
            assertThat(candidate.mapId()).isEqualTo(100);
            assertThat(candidate.score()).isEqualTo(1.0d);
            assertThat(candidate.iconUrl()).isNull();
            assertThat(candidate.nameKr()).isNull();
            assertThat(candidate.bookmarkId()).isEqualTo(17);
        });
        verify(bookmarkFlagService).findBookmarkIds("member", BookmarkType.MAP, List.of(100));
    }

    private Map map(int mapId, String name) {
        return new Map(mapId, name, null, null, null, null, null, "icon-" + mapId);
    }
}
