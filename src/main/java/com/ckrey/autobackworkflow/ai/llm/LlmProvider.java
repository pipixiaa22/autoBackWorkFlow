package com.ckrey.autobackworkflow.ai.llm;

import com.ckrey.autobackworkflow.ai.model.AnalysisCandidate;

/** Port for structured dialogue analysis. Vendor adapters belong behind this interface. */
public interface LlmProvider {
    String providerCode();
    AnalysisCandidate analyse(LlmAnalysisCommand command);
    record LlmAnalysisCommand(String storyBackground, String originalDialogue, String rewriteMode, String prompt) { }
}
