package com.ckrey.autobackworkflow.config;

import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonConfigurationTests {

    @Test
    void serializesLongIdsAsExactJsonStrings() throws Exception {
        long id = 2_101_841_628_044_984_322L;
        AdsProject project = new AdsProject();
        project.setId(id);

        ObjectMapper mapper = new JacksonConfiguration().objectMapper();
        JsonNode response = mapper.readTree(mapper.writeValueAsString(ApiResponse.ok(project)));

        JsonNode idNode = response.path("data").path("id");
        assertTrue(idNode.isTextual());
        assertEquals("2101841628044984322", idNode.textValue());
    }

    @Test
    void serializesLongIdsAsExactJsonStringsWithSpringBootMvcMapper() throws Exception {
        long id = 2_101_841_628_044_984_322L;
        AdsProject project = new AdsProject();
        project.setId(id);

        tools.jackson.databind.json.JsonMapper.Builder builder = tools.jackson.databind.json.JsonMapper.builder();
        new JacksonConfiguration().jsonMapperBuilderCustomizer().customize(builder);
        tools.jackson.databind.ObjectMapper mapper = builder.build();
        tools.jackson.databind.JsonNode response =
                mapper.readTree(mapper.writeValueAsString(ApiResponse.ok(project)));

        tools.jackson.databind.JsonNode idNode = response.path("data").path("id");
        assertTrue(idNode.isString());
        assertEquals("2101841628044984322", idNode.stringValue());
    }
}
