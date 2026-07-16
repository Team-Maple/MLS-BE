package com.maple.api.map.application;

import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.map.application.dto.MapRecommendationResultDto;
import com.maple.api.job.exception.JobException;
import com.maple.api.job.repository.JobRepository;
import com.maple.api.map.exception.MapException;
import com.maple.api.common.config.RecommendationProperties;
import com.maple.api.map.domain.RecommendationCandidate;
import com.maple.api.map.domain.RecommendationReason;
import com.maple.api.map.repository.MapRecommendationRepository;
import com.maple.api.map.domain.RecommendationEngineType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MapRecommendationServiceTest {

    private JobRepository jobRepository;
    private MapRecommendationRepository aura;
    private MapRecommendationRepository mysql;
    private MapRecommendationEnrichmentService enrichmentService;
    private MapRecommendationObservability observability;
    private RecommendationProperties properties;
    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;
    private MapRecommendationService service;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        aura = engine(RecommendationEngineType.AURA);
        mysql = engine(RecommendationEngineType.MYSQL);
        enrichmentService = mock(MapRecommendationEnrichmentService.class);
        observability = mock(MapRecommendationObservability.class);
        properties = new RecommendationProperties();
        properties.setV2Enabled(true);
        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        MapRecommendationQueryExecutor queryExecutor = new MapRecommendationQueryExecutor(
                jobRepository,
                new MapRecommendationEngineRouter(List.of(aura, mysql)),
                enrichmentService,
                transactionManager,
                properties
        );
        service = new MapRecommendationService(
                properties,
                queryExecutor,
                observability
        );
        clearInvocations(aura, mysql);
    }

    @Test
    void v1UsesConfiguredAuraAndDefaultLimitFive() {
        when(jobRepository.existsById(110)).thenReturn(true);
        RecommendationCandidate candidate = new RecommendationCandidate(100L, 0.95d, List.of());
        when(aura.findRecommendations(45, 110, 5)).thenReturn(List.of(candidate));
        when(enrichmentService.enrich("member", List.of(candidate))).thenReturn(List.of(
                new MapRecommendationResultDto(100, 0.95d, "icon", "map", 7, List.of())
        ));

        var result = service.recommendV1("member", 45, 110, null);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.mapId()).isEqualTo(100);
            assertThat(item.score()).isEqualTo(0.95d);
            assertThat(item.bookmarkId()).isEqualTo(7);
        });
        verify(aura).findRecommendations(45, 110, 5);
        verify(transactionManager).getTransaction(org.mockito.ArgumentMatchers.argThat(definition ->
                definition.isReadOnly()
                        && definition.getTimeout() == TransactionDefinition.TIMEOUT_DEFAULT));
        verify(observability).completed(
                org.mockito.ArgumentMatchers.eq(RecommendationEngineType.AURA),
                org.mockito.ArgumentMatchers.eq("v1"),
                org.mockito.ArgumentMatchers.eq(1),
                anyLong()
        );
    }

    @Test
    void v1CanSelectMysqlWithoutDualReadOrFallback() {
        properties.setV1Engine("mysql");
        when(jobRepository.existsById(110)).thenReturn(true);
        when(mysql.findRecommendations(45, 110, 20)).thenReturn(List.of());
        when(enrichmentService.enrich(null, List.of())).thenReturn(List.of());

        assertThat(service.recommendV1(null, 45, 110, 20)).isEmpty();

        verify(mysql).findRecommendations(45, 110, 20);
        verify(transactionManager).getTransaction(org.mockito.ArgumentMatchers.argThat(definition ->
                definition.isReadOnly()
                        && definition.getTimeout() == properties.getQueryTimeoutSeconds()));
        verifyNoInteractions(aura);
    }

    @Test
    void v2AlwaysUsesMysqlAndMapsNonNullReasons() {
        when(jobRepository.existsById(110)).thenReturn(true);
        RecommendationCandidate candidate = new RecommendationCandidate(
                100L,
                0.95d,
                List.of(new RecommendationReason("reward", "xp"))
        );
        when(mysql.findRecommendations(45, 110, 1)).thenReturn(List.of(candidate));
        when(enrichmentService.enrich(null, List.of(candidate))).thenReturn(List.of(
                new MapRecommendationResultDto(
                        100,
                        0.95d,
                        "icon",
                        "map",
                        null,
                        List.of(new RecommendationReason("reward", "xp"))
                )
        ));

        var result = service.recommendV2(null, 45, 110, 1);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.reasons()).isNotNull();
            assertThat(item.reasons()).singleElement().satisfies(reason -> {
                assertThat(reason.axis()).isEqualTo("reward");
                assertThat(reason.value()).isEqualTo("xp");
            });
        });
        verifyNoInteractions(aura);
        verify(transactionManager, atLeastOnce()).getTransaction(
                org.mockito.ArgumentMatchers.argThat(definition ->
                        definition.getTimeout() == properties.getQueryTimeoutSeconds()));
    }

    @Test
    void missingJobKeepsExistingNotFoundContract() {
        when(jobRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> service.recommendV2(null, 45, 999, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getExceptionCode()).isEqualTo(JobException.JOB_NOT_FOUND));

        verifyNoInteractions(mysql, enrichmentService, observability);
    }

    @Test
    void selectedEngineFailureBecomesRecommendationUnavailableWithoutFallback() {
        when(jobRepository.existsById(110)).thenReturn(true);
        RuntimeException databaseFailure = new RuntimeException("schema missing");
        when(mysql.findRecommendations(45, 110, 5)).thenThrow(databaseFailure);

        assertThatThrownBy(() -> service.recommendV2(null, 45, 110, null))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getExceptionCode()).isEqualTo(MapException.MAP_RECOMMENDATION_UNAVAILABLE);
                    assertThat(exception.getCause()).isSameAs(databaseFailure);
                });

        verify(observability).unavailable(
                org.mockito.ArgumentMatchers.eq(RecommendationEngineType.MYSQL),
                org.mockito.ArgumentMatchers.eq("v2"),
                anyLong(),
                org.mockito.ArgumentMatchers.same(databaseFailure)
        );
        verifyNoInteractions(aura, enrichmentService);
    }

    @Test
    void invalidV1EngineConfigurationFailsOnlyTheEndpoint() {
        properties.setV1Engine("not-an-engine");

        assertThatThrownBy(() -> service.recommendV1(null, 45, 110, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getExceptionCode()).isEqualTo(MapException.MAP_RECOMMENDATION_UNAVAILABLE));

        verifyNoInteractions(jobRepository, aura, mysql, enrichmentService, observability);
    }

    @Test
    void v2KillSwitchReturnsUnavailableWithoutTouchingDatabaseOrEngine() {
        properties.setV2Enabled(false);

        assertThatThrownBy(() -> service.recommendV2(null, 45, 110, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getExceptionCode())
                                .isEqualTo(MapException.MAP_RECOMMENDATION_UNAVAILABLE));

        verify(observability).disabled(RecommendationEngineType.MYSQL, "v2");
        verifyNoInteractions(jobRepository, aura, mysql, enrichmentService);
    }

    @Test
    void transactionStartFailureBecomesUnavailableAndIsObserved() {
        CannotCreateTransactionException transactionFailure =
                new CannotCreateTransactionException("database unavailable");
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenThrow(transactionFailure);

        assertThatThrownBy(() -> service.recommendV2(null, 45, 110, null))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getExceptionCode())
                            .isEqualTo(MapException.MAP_RECOMMENDATION_UNAVAILABLE);
                    assertThat(exception.getCause()).isSameAs(transactionFailure);
                });

        verify(observability).unavailable(
                org.mockito.ArgumentMatchers.eq(RecommendationEngineType.MYSQL),
                org.mockito.ArgumentMatchers.eq("v2"),
                anyLong(),
                org.mockito.ArgumentMatchers.same(transactionFailure)
        );
    }

    private MapRecommendationRepository engine(RecommendationEngineType type) {
        MapRecommendationRepository repository = mock(MapRecommendationRepository.class);
        when(repository.engineType()).thenReturn(type);
        return repository;
    }
}
