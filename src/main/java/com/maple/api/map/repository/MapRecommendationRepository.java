package com.maple.api.map.repository;

import com.maple.api.map.domain.RecommendationCandidate;
import com.maple.api.map.domain.RecommendationEngineType;

import java.util.List;

public interface MapRecommendationRepository {

    RecommendationEngineType engineType();

    List<RecommendationCandidate> findRecommendations(int level, int jobId, int limit);
}
