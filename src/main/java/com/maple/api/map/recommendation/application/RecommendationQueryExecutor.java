package com.maple.api.map.recommendation.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.job.exception.JobException;
import com.maple.api.job.repository.JobRepository;
import com.maple.api.map.recommendation.config.RecommendationProperties;
import com.maple.api.map.recommendation.domain.RecommendationCandidate;
import com.maple.api.map.recommendation.port.RecommendationEnginePort;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/**
 * Programmatic transaction boundary invoked from the non-transactional API service. Transaction
 * start and datasource failures stay inside the caller's 503 boundary. The bounded timeout is
 * applied only to MySQL evidence reads; Aura keeps the legacy no-timeout relational transaction.
 */
@Service
@RequiredArgsConstructor
public class RecommendationQueryExecutor {

    private final JobRepository jobRepository;
    private final RecommendationEngineRouter engineRouter;
    private final RecommendationEnrichmentService enrichmentService;
    private final PlatformTransactionManager transactionManager;
    private final RecommendationProperties recommendationProperties;

    public List<EnrichedRecommendationCandidate> execute(
            String memberId,
            int level,
            int jobId,
            int limit,
            RecommendationEngineType engine
    ) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        if (engine == RecommendationEngineType.MYSQL) {
            transaction.setTimeout(recommendationProperties.getQueryTimeoutSeconds());
        }
        return Objects.requireNonNull(transaction.execute(status -> executeWithinTransaction(
                memberId,
                level,
                jobId,
                limit,
                engine
        )));
    }

    private List<EnrichedRecommendationCandidate> executeWithinTransaction(
            String memberId,
            int level,
            int jobId,
            int limit,
            RecommendationEngineType engine
    ) {
        if (!jobRepository.existsById(jobId)) {
            throw ApiException.of(JobException.JOB_NOT_FOUND);
        }

        RecommendationEnginePort enginePort = engineRouter.find(engine)
                .orElseThrow(() -> new IllegalStateException(
                        "Recommendation engine is not configured: " + engine
                ));
        List<RecommendationCandidate> candidates = enginePort.recommend(level, jobId, limit);
        return enrichmentService.enrich(memberId, candidates);
    }
}
