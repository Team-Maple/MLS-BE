package com.maple.api.map.presentation;

import com.maple.api.map.exception.MapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:recommendation-unavailable;DB_CLOSE_DELAY=-1;MODE=MYSQL",
                "recommendation.v1-engine=MYSQL"
        }
)
class RecommendationSchemaUnavailableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertExistingJobWithoutRecommendationSchema() {
        jdbcTemplate.update("""
                INSERT INTO jobs(job_id, job_name, job_level, parent_job_id, disabled)
                VALUES (111, 'Crusader', 3, NULL, FALSE)
                """);
    }

    @Test
    void selectedMysqlEngineReturns503WhileApplicationAndOtherEndpointsRemainAvailable() throws Exception {
        mockMvc.perform(get("/api/v2/maps/recommendations")
                        .param("level", "50")
                        .param("jobId", "111"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value(MapException.MAP_RECOMMENDATION_UNAVAILABLE.getMessage()));

        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "50")
                        .param("jobId", "111"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value(MapException.MAP_RECOMMENDATION_UNAVAILABLE.getMessage()));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
