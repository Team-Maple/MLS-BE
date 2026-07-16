package com.maple.api.map.presentation;

import com.maple.api.job.repository.JobRepository;
import com.maple.api.map.exception.MapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationJobLookupUnavailableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobRepository jobRepository;

    @BeforeEach
    void failJobLookup() {
        when(jobRepository.existsById(anyInt()))
                .thenThrow(new DataAccessResourceFailureException("datasource unavailable"));
    }

    @Test
    void jobDatasourceFailureReturnsRecommendation503ForV1AndV2() throws Exception {
        for (String path : new String[]{
                "/api/v1/maps/recommendations",
                "/api/v2/maps/recommendations"
        }) {
            mockMvc.perform(get(path)
                            .param("level", "50")
                            .param("jobId", "110"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message")
                            .value(MapException.MAP_RECOMMENDATION_UNAVAILABLE.getMessage()));
        }
    }
}
