package com.maple.api.map.recommendation.adapter;

import com.maple.api.map.recommendation.domain.RecommendationCandidate;
import com.maple.api.map.recommendation.port.RecommendationEnginePort;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@ConditionalOnBean(Driver.class)
@RequiredArgsConstructor
public class AuraRecommendationEngineAdapter implements RecommendationEnginePort {

    private static final String RECOMMEND_QUERY = """
            MATCH (lvl:Level {value: $level})-[rl:RECO {dim:'LEVEL'}]->(m:Map)
            OPTIONAL MATCH (j:Job)-[rj:RECO {dim:'JOB'}]->(m)
            WHERE j.job_id IN [$jobId, 0]
            WITH m,
                 coalesce(rl.hit_count_level, 0) AS levelHits,
                 coalesce(sum(
                     CASE
                         WHEN j.job_id = $jobId THEN coalesce(rj.hit_count_job, 0)
                         ELSE coalesce(rj.hit_count_job, 0) * 0.5
                     END
                 ), 0) AS jobHits
            WITH m, levelHits, jobHits,
                 levelHits * 0.8 + jobHits * 0.2 AS score
            RETURN m.map_id AS mapId,
                   score
            ORDER BY score DESC
            LIMIT $limit
            """;

    private final Driver auraDbDriver;

    @Override
    public RecommendationEngineType engineType() {
        return RecommendationEngineType.AURA;
    }

    @Override
    public List<RecommendationCandidate> recommend(int level, int jobId, int limit) {
        try (Session session = auraDbDriver.session(SessionConfig.defaultConfig())) {
            Map<String, Object> params = new HashMap<>();
            params.put("level", level);
            params.put("jobId", jobId);
            params.put("limit", limit);

            return session.executeRead(tx -> {
                Result result = tx.run(RECOMMEND_QUERY, params);
                List<RecommendationCandidate> recommendations = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    recommendations.add(new RecommendationCandidate(
                            record.get("mapId").asLong(),
                            record.get("score").asDouble(),
                            List.of()
                    ));
                }
                return recommendations;
            });
        }
    }

    public void ping() {
        try (Session session = auraDbDriver.session(SessionConfig.defaultConfig())) {
            session.run("RETURN 1").consume();
        }
    }
}
