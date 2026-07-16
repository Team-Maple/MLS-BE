package com.maple.api.map.recommendation.port;

import com.maple.api.map.recommendation.domain.RecommendationCandidate;

import java.util.List;

public interface RecommendationEnginePort {

    RecommendationEngineType engineType();

    List<RecommendationCandidate> recommend(int level, int jobId, int limit);
}
