package com.ckrey.autobackworkflow.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ckrey.autobackworkflow.config.JacksonConfiguration;
import com.ckrey.autobackworkflow.domain.AdsAnalysisRun;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class AnalysisDtosTests {
    @Test
    void excludesLargeInputSnapshotsAndRawProviderPayloadFromClientView() throws Exception {
        AdsAnalysisRun run = new AdsAnalysisRun();
        run.setId(900000000000000201L);
        run.setDialogueSnapshot("very large source dialogue");
        run.setBackgroundSnapshot("private background");
        run.setRawResponseJson("private provider payload");
        run.setCandidateResultJson("{\"segments\":[]}");

        JsonNode json = new JacksonConfiguration().objectMapper()
                .readTree(new JacksonConfiguration().objectMapper()
                        .writeValueAsString(AnalysisDtos.AnalysisRunView.from(run)));

        assertEquals("900000000000000201", json.path("id").asText());
        assertEquals("{\"segments\":[]}", json.path("candidateResultJson").asText());
        assertFalse(json.has("dialogueSnapshot"));
        assertFalse(json.has("backgroundSnapshot"));
        assertFalse(json.has("rawResponseJson"));
    }
}
