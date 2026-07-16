package com.maple.api.map.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.api.auth.domain.PrincipalDetails;
import com.maple.api.common.presentation.exception.ApiException;
import com.maple.api.job.exception.JobException;
import com.maple.api.map.application.dto.MapRecommendationDto;
import com.maple.api.map.application.dto.MapRecommendationReasonDto;
import com.maple.api.map.application.dto.MapRecommendationV2Dto;
import com.maple.api.map.exception.MapException;
import com.maple.api.map.application.MapRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MapRecommendationControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MapRecommendationService recommendationService;

    @BeforeEach
    void resetMock() {
        reset(recommendationService);
    }

    @Test
    void v1KeepsExactOuterAndItemKeysWithoutReasonsForAnonymousCaller() throws Exception {
        when(recommendationService.recommendV1(null, 45, 110, 5)).thenReturn(List.of(
                new MapRecommendationDto(100000000, 0.95d, "https://icon", "헤네시스", null)
        ));

        String json = mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45")
                        .param("jobId", "110")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").isEmpty())
                .andExpect(jsonPath("$.data[0].bookmarkId").isEmpty())
                .andExpect(jsonPath("$.data[0].reasons").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        assertThat(fieldNames(root)).containsExactlyInAnyOrder("success", "code", "message", "data");
        assertThat(root.get("message").isNull()).isTrue();
        assertThat(fieldNames(root.get("data").get(0))).containsExactlyInAnyOrder(
                "mapId", "score", "iconUrl", "nameKr", "bookmarkId"
        );
        verify(recommendationService).recommendV1(null, 45, 110, 5);
    }

    @Test
    void v1PassesLoggedInMemberAndReturnsBookmark() throws Exception {
        when(recommendationService.recommendV1("member-1", 45, 110, 5)).thenReturn(List.of(
                new MapRecommendationDto(100, 1.0d, "icon", "map", 77)
        ));

        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45")
                        .param("jobId", "110")
                        .param("limit", "5")
                        .with(user(new PrincipalDetails("member-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bookmarkId").value(77));

        verify(recommendationService).recommendV1("member-1", 45, 110, 5);
    }

    @Test
    void v1SupportsContractLimitBoundsAndRejectsInvalidLimits() throws Exception {
        when(recommendationService.recommendV1(null, 45, 110, 1)).thenReturn(List.of());
        when(recommendationService.recommendV1(null, 45, 110, 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "110").param("limit", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "110").param("limit", "20"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "110").param("limit", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "110").param("limit", "21"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void v1KeepsEmptyNotFoundAndUnavailableContracts() throws Exception {
        when(recommendationService.recommendV1(null, 45, 110, 5)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        when(recommendationService.recommendV1(null, 45, 999, 5))
                .thenThrow(ApiException.of(JobException.JOB_NOT_FOUND));
        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(JobException.JOB_NOT_FOUND.getMessage()));

        when(recommendationService.recommendV1(null, 45, 500, 5))
                .thenThrow(ApiException.of(MapException.MAP_RECOMMENDATION_UNAVAILABLE));
        mockMvc.perform(get("/api/v1/maps/recommendations")
                        .param("level", "45").param("jobId", "500"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(MapException.MAP_RECOMMENDATION_UNAVAILABLE.getMessage()));
    }

    @Test
    void v2IsAnonymousAndReturnsSeparateDtoWithNonNullReasonsArrays() throws Exception {
        when(recommendationService.recommendV2(null, 45, 110, 5)).thenReturn(List.of(
                new MapRecommendationV2Dto(
                        100,
                        0.95d,
                        "icon",
                        "map",
                        null,
                        List.of(new MapRecommendationReasonDto("reward", "xp"))
                ),
                new MapRecommendationV2Dto(101, 0.9d, "icon2", "map2", null, List.of())
        ));

        String json = mockMvc.perform(get("/api/v2/maps/recommendations")
                        .param("level", "45")
                        .param("jobId", "110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reasons").isArray())
                .andExpect(jsonPath("$.data[0].reasons[0].axis").value("reward"))
                .andExpect(jsonPath("$.data[0].reasons[0].value").value("xp"))
                .andExpect(jsonPath("$.data[1].reasons").isArray())
                .andExpect(jsonPath("$.data[1].reasons").isEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode first = objectMapper.readTree(json).get("data").get(0);
        assertThat(fieldNames(first)).containsExactlyInAnyOrder(
                "mapId", "score", "iconUrl", "nameKr", "bookmarkId", "reasons"
        );
        assertThat(fieldNames(first.get("reasons").get(0))).containsExactlyInAnyOrder("axis", "value");
    }

    private Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        Iterator<String> iterator = node.fieldNames();
        iterator.forEachRemaining(names::add);
        return names;
    }
}
