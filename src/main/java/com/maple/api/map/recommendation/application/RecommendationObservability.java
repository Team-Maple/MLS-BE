package com.maple.api.map.recommendation.application;

import com.maple.api.common.logging.SafeExceptionLog;
import com.maple.api.map.recommendation.port.RecommendationEngineType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecommendationObservability {

    static final String REQUESTS_METRIC = "mapleland.recommendation.requests";
    static final String RESULTS_METRIC = "mapleland.recommendation.results";

    private final MeterRegistry meterRegistry;

    public RecommendationObservability(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void completed(
            RecommendationEngineType engine,
            String apiVersion,
            int resultCount,
            long durationNanos
    ) {
        String outcome = resultCount == 0 ? "empty" : "success";
        counter(engine, apiVersion, outcome).increment();
        resultSummary(engine, apiVersion).record(resultCount);

        log.atInfo()
                .addKeyValue("event.action", "recommendation.query")
                .addKeyValue("event.outcome", "success")
                .addKeyValue("mapleland.recommendation.engine", engine.metricValue())
                .addKeyValue("mapleland.api.version", apiVersion)
                .addKeyValue("event.duration", durationNanos)
                .addKeyValue("mapleland.result.count", resultCount)
                .log("Recommendation query completed");
    }

    public void unavailable(
            RecommendationEngineType engine,
            String apiVersion,
            long durationNanos,
            Throwable exception
    ) {
        counter(engine, apiVersion, "unavailable").increment();

        SafeExceptionLog.addException(log.atWarn(), exception)
                .addKeyValue("event.action", "recommendation.query")
                .addKeyValue("event.outcome", "failure")
                .addKeyValue("mapleland.recommendation.engine", engine.metricValue())
                .addKeyValue("mapleland.api.version", apiVersion)
                .addKeyValue("event.duration", durationNanos)
                .addKeyValue("mapleland.result.count", 0)
                .log("Recommendation query unavailable");
    }

    public void disabled(RecommendationEngineType engine, String apiVersion) {
        counter(engine, apiVersion, "unavailable").increment();
    }

    private Counter counter(RecommendationEngineType engine, String apiVersion, String outcome) {
        return Counter.builder(REQUESTS_METRIC)
                .description("Recommendation endpoint outcomes by selected engine and API version")
                .tag("engine", engine.metricValue())
                .tag("api_version", apiVersion)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private DistributionSummary resultSummary(RecommendationEngineType engine, String apiVersion) {
        return DistributionSummary.builder(RESULTS_METRIC)
                .description("Number of recommendation items returned per successful request")
                .baseUnit("recommendations")
                .tag("engine", engine.metricValue())
                .tag("api_version", apiVersion)
                .register(meterRegistry);
    }
}
