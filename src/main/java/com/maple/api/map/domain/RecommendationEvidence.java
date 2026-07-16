package com.maple.api.map.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable projection of an APPROVED reviewed claim returned for the requested job lineage.
 * Approval and lineage filtering belong to the recommendation repository; this type deliberately carries
 * no mutable review state.
 */
public record RecommendationEvidence(
        long extractedClaimId,
        long reviewedClaimId,
        long mapId,
        long jobId,
        Integer levelMin,
        Integer levelMax,
        Polarity polarity,
        LocalDateTime publishedAt,
        int patchCount,
        List<RecommendationFacet> reasonFacets
) {

    public RecommendationEvidence {
        Objects.requireNonNull(polarity, "polarity must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        Objects.requireNonNull(reasonFacets, "reasonFacets must not be null");
        if (patchCount < 0) {
            throw new IllegalArgumentException("patchCount must not be negative");
        }
        reasonFacets = List.copyOf(new LinkedHashSet<>(reasonFacets));
    }
}
