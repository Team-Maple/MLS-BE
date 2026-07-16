package com.maple.api.map.recommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationScoringServiceTest {

    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2025, 1, 1, 12, 0);

    private final RecommendationScoringService scoringService = new RecommendationScoringService();

    @Test
    void sumsPositiveAndNegativeEvidenceWithPatchFreshness() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 100, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT),
                evidence(3, 3, 100, 110, 40, 50, Polarity.NEGATIVE, 1, PUBLISHED_AT)
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.mapId()).isEqualTo(100);
            assertThat(candidate.score()).isCloseTo(0.95d, within(0.000_000_1d));
        });
    }

    @Test
    void matchesInclusiveLevelRangesAndInfersMissingBoundByTenLevels() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 100, 110, 50, null, Polarity.POSITIVE, 1, PUBLISHED_AT),
                evidence(3, 3, 100, 110, null, 50, Polarity.POSITIVE, 2, PUBLISHED_AT),
                evidence(4, 4, 100, 110, 51, null, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(5, 5, 100, 110, null, 49, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(6, 6, 100, 110, null, null, Polarity.POSITIVE, 0, PUBLISHED_AT)
        );

        List<RecommendationCandidate> result = scoringService.score(50, evidence, 5);

        assertThat(result).singleElement()
                .extracting(RecommendationCandidate::score)
                .isEqualTo(2.85d);
    }

    @Test
    void floorsFreshnessAtPointOneAndAlwaysReturnsFiniteScores() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 1, 200, Polarity.POSITIVE, 18, PUBLISHED_AT),
                evidence(2, 2, 100, 110, 1, 200, Polarity.POSITIVE, 19, PUBLISHED_AT),
                evidence(3, 3, 100, 110, 1, 200, Polarity.POSITIVE, Integer.MAX_VALUE, PUBLISHED_AT)
        );

        RecommendationCandidate candidate = scoringService.score(100, evidence, 5).getFirst();

        assertThat(candidate.score()).isEqualTo(0.3d);
        assertThat(Double.isFinite(candidate.score())).isTrue();
    }

    @Test
    void deduplicatesLineageFanoutByExtractedClaimAndMapUsingLowestReviewId() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(10, 20, 100, 112, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(10, 10, 100, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT),
                evidence(10, 30, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT)
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result)
                .extracting(RecommendationCandidate::mapId, RecommendationCandidate::score)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, 1.0d),
                        org.assertj.core.groups.Tuple.tuple(100L, 0.9d)
                );
    }

    @Test
    void requiresMatchedPositiveEvidenceAndPositiveNetScore() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.NEGATIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(3, 3, 101, 110, 40, 50, Polarity.NEGATIVE, 0, PUBLISHED_AT),
                evidence(4, 4, 102, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT),
                evidence(5, 5, 102, 110, 40, 50, Polarity.NEGATIVE, 0, PUBLISHED_AT),
                evidence(6, 6, 103, 110, 40, 50, Polarity.POSITIVE, 19, PUBLISHED_AT)
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).singleElement()
                .extracting(RecommendationCandidate::mapId, RecommendationCandidate::score)
                .containsExactly(103L, 0.1d);
    }

    @Test
    void aggregatesSignedFacetWeightsOmitsNonPositiveAxesAndUsesStableAxisOrder() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT,
                        RecommendationFacet.REWARD_XP,
                        RecommendationFacet.PLAY_STYLE_SOLO,
                        RecommendationFacet.OPERABILITY_FATIGUE),
                evidence(2, 2, 100, 110, 40, 50, Polarity.NEGATIVE, 0, PUBLISHED_AT,
                        RecommendationFacet.REWARD_XP,
                        RecommendationFacet.OPERABILITY_FATIGUE),
                evidence(3, 3, 100, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT,
                        RecommendationFacet.REWARD_MESO,
                        RecommendationFacet.PLAY_STYLE_PARTY)
        );

        RecommendationCandidate candidate = scoringService.score(45, evidence, 5).getFirst();

        assertThat(candidate.reasons()).containsExactly(
                new RecommendationReason("reward", "meso"),
                new RecommendationReason("play_style", "solo")
        );
    }

    @Test
    void usesFacetPriorityWhenAccumulatedWeightsTie() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT,
                        RecommendationFacet.REWARD_XP,
                        RecommendationFacet.PLAY_STYLE_PARTY,
                        RecommendationFacet.OPERABILITY_BUDGET),
                evidence(2, 2, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT,
                        RecommendationFacet.REWARD_MESO,
                        RecommendationFacet.PLAY_STYLE_SOLO,
                        RecommendationFacet.OPERABILITY_MOBILITY)
        );

        RecommendationCandidate candidate = scoringService.score(45, evidence, 5).getFirst();

        assertThat(candidate.reasons()).containsExactly(
                new RecommendationReason("reward", "xp"),
                new RecommendationReason("play_style", "solo"),
                new RecommendationReason("operability", "mobility")
        );
    }

    @Test
    void sortsHigherScoreFirstEvenWhenLowerScoreHasHigherFreshnessSum() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT.plusDays(1)),
                evidence(3, 3, 101, 110, 40, 50, Polarity.NEGATIVE, 2, PUBLISHED_AT.plusDays(1))
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).extracting(RecommendationCandidate::mapId).containsExactly(100L, 101L);
        assertThat(result).extracting(RecommendationCandidate::score).containsExactly(1.0d, 0.1d);
    }

    @Test
    void breaksEqualScoreByHigherFreshnessSum() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(3, 3, 101, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT),
                evidence(4, 4, 101, 110, 40, 50, Polarity.NEGATIVE, 2, PUBLISHED_AT)
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).extracting(RecommendationCandidate::mapId).containsExactly(101L, 100L);
    }

    @Test
    void representativeUsesNewestEvidenceWhenHighestContributionsTie() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT.plusDays(3)),
                evidence(3, 3, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(4, 4, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT.plusDays(2))
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).extracting(RecommendationCandidate::mapId).containsExactly(100L, 101L);
    }

    @Test
    void representativeDateComesFromHighestContributionRatherThanNewestEvidence() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 100, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT.plusDays(10)),
                evidence(3, 3, 101, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT.plusDays(1)),
                evidence(4, 4, 101, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT.minusDays(1))
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).extracting(RecommendationCandidate::mapId).containsExactly(101L, 100L);
    }

    @Test
    void breaksRemainingTiesByMapIdAscending() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 200, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT),
                evidence(2, 2, 100, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT)
        );

        List<RecommendationCandidate> result = scoringService.score(45, evidence, 5);

        assertThat(result).extracting(RecommendationCandidate::mapId).containsExactly(100L, 200L);
    }

    @Test
    void appliesLimitOnlyAfterFinalSortingAndAcceptsContractBounds() {
        List<RecommendationEvidence> evidence = List.of(
                evidence(1, 1, 100, 110, 40, 50, Polarity.POSITIVE, 2, PUBLISHED_AT),
                evidence(2, 2, 101, 110, 40, 50, Polarity.POSITIVE, 1, PUBLISHED_AT),
                evidence(3, 3, 102, 110, 40, 50, Polarity.POSITIVE, 0, PUBLISHED_AT)
        );

        assertThat(scoringService.score(45, evidence, 1))
                .extracting(RecommendationCandidate::mapId)
                .containsExactly(102L);
        assertThat(scoringService.score(45, evidence, 20)).hasSize(3);
        assertThatThrownBy(() -> scoringService.score(45, evidence, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> scoringService.score(45, evidence, 21))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void returnedCollectionsAreImmutableSnapshots() {
        ArrayList<RecommendationFacet> mutableFacets = new ArrayList<>();
        mutableFacets.add(RecommendationFacet.REWARD_XP);
        RecommendationEvidence row = new RecommendationEvidence(
                1, 1, 100, 110, 40, 50, Polarity.POSITIVE, PUBLISHED_AT, 0, mutableFacets
        );
        mutableFacets.clear();

        List<RecommendationCandidate> result = scoringService.score(45, List.of(row), 5);

        assertThat(result.getFirst().reasons()).containsExactly(new RecommendationReason("reward", "xp"));
        assertThatThrownBy(() -> result.add(result.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.getFirst().reasons().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private RecommendationEvidence evidence(
            long extractedClaimId,
            long reviewedClaimId,
            long mapId,
            long jobId,
            Integer levelMin,
            Integer levelMax,
            Polarity polarity,
            int patchCount,
            LocalDateTime publishedAt,
            RecommendationFacet... facets
    ) {
        return new RecommendationEvidence(
                extractedClaimId,
                reviewedClaimId,
                mapId,
                jobId,
                levelMin,
                levelMax,
                polarity,
                publishedAt,
                patchCount,
                List.of(facets)
        );
    }
}
