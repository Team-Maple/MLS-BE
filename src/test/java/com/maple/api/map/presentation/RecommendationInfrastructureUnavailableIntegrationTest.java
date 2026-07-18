package com.maple.api.map.presentation;

import com.maple.api.map.exception.MapException;
import com.maple.api.map.application.MapRecommendationQueryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationInfrastructureUnavailableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MapRecommendationQueryExecutor queryExecutor;

    @BeforeEach
    void failAtTransactionalProxyBoundary() {
        when(queryExecutor.execute(isNull(), anyInt(), anyInt(), anyInt(), any()))
                .thenThrow(new CannotCreateTransactionException("database unavailable"));
    }

    @Test
    void transactionStartFailureReturnsRecommendation503ForV1AndV2() throws Exception {
        assertUnavailable("/api/v1/maps/recommendations");
        assertUnavailable("/api/v2/maps/recommendations");
    }

    private void assertUnavailable(String path) throws Exception {
        mockMvc.perform(get(path)
                        .param("level", "50")
                        .param("jobId", "111"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value(MapException.MAP_RECOMMENDATION_UNAVAILABLE.getMessage()));
    }
}
