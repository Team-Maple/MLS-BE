package com.maple.api.map.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maple.api.map.recommendation.application.MapRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MapRecommendationOpenApiTest {

    private static final String V1_SCHEMA = "com.maple.api.map.application.dto.MapRecommendationDto";
    private static final String V2_SCHEMA = "com.maple.api.map.application.dto.MapRecommendationV2Dto";
    private static final String REASON_SCHEMA = "com.maple.api.map.application.dto.MapRecommendationReasonDto";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MapRecommendationService recommendationService;

    @Test
    void documentsV1ExactSchemaAndEvidenceScoreSemanticChange() throws Exception {
        JsonNode document = openApi();
        JsonNode operation = document.at("/paths/~1api~1v1~1maps~1recommendations/get");

        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.at("/responses/503").isMissingNode()).isFalse();
        assertLimitContract(operation);
        assertOptionalAuthentication(operation);

        JsonNode schema = document.path("components").path("schemas").path(V1_SCHEMA);
        assertThat(fieldNames(schema.path("properties"))).containsExactlyInAnyOrder(
                "mapId", "score", "iconUrl", "nameKr", "bookmarkId"
        );
        assertThat(schema.at("/properties/score/description").asText())
                .contains("evidence net score")
                .contains("APPROVED");
        assertThat(schema.path("properties").has("reasons")).isFalse();
    }

    @Test
    void documentsV2ReasonsAsSeparateStableCodeSchema() throws Exception {
        JsonNode document = openApi();
        JsonNode operation = document.at("/paths/~1api~1v2~1maps~1recommendations/get");

        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.at("/responses/503").isMissingNode()).isFalse();
        assertLimitContract(operation);
        assertOptionalAuthentication(operation);

        JsonNode v2Schema = document.path("components").path("schemas").path(V2_SCHEMA);
        assertThat(fieldNames(v2Schema.path("properties"))).containsExactlyInAnyOrder(
                "mapId", "score", "iconUrl", "nameKr", "bookmarkId", "reasons"
        );
        assertThat(v2Schema.at("/properties/reasons/type").asText()).isEqualTo("array");
        assertThat(v2Schema.at("/properties/reasons/items/$ref").asText()).endsWith(REASON_SCHEMA);

        JsonNode reasonSchema = document.path("components").path("schemas").path(REASON_SCHEMA);
        assertThat(strings(reasonSchema.at("/properties/axis/enum")))
                .containsExactly("reward", "play_style", "operability");
        assertThat(strings(reasonSchema.at("/properties/value/enum")))
                .containsExactly("xp", "meso", "loot", "solo", "party", "party_quest", "fatigue", "mobility", "budget");
    }

    private JsonNode openApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private void assertLimitContract(JsonNode operation) {
        JsonNode limit = findParameter(operation, "limit");
        assertThat(limit.path("required").asBoolean()).isFalse();
        assertThat(limit.at("/schema/default").asInt()).isEqualTo(5);
        assertThat(limit.at("/schema/minimum").asInt()).isEqualTo(1);
        assertThat(limit.at("/schema/maximum").asInt()).isEqualTo(20);
    }

    private void assertOptionalAuthentication(JsonNode operation) {
        JsonNode security = operation.path("security");
        assertThat(security.isArray()).isTrue();
        assertThat(security).anySatisfy(requirement -> assertThat(requirement.isEmpty()).isTrue());
        assertThat(security).anySatisfy(requirement ->
                assertThat(requirement.has("Authorization")).isTrue());
    }

    private JsonNode findParameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("Missing OpenAPI parameter: " + name);
    }

    private Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        Iterator<String> iterator = node.fieldNames();
        iterator.forEachRemaining(names::add);
        return names;
    }

    private List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }
}
