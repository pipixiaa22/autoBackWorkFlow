package com.ckrey.autobackworkflow.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ckrey.autobackworkflow.common.exception.BizException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleBasedLlmProviderTests {
    private final RuleBasedLlmProvider provider = new RuleBasedLlmProvider();

    @Test
    void splitsWithConfiguredPunctuationAndMaximumLength() {
        var result = provider.analyse(new LlmProvider.LlmAnalysisCommand("", "甲，乙，丙丁戊", "STRICT", "", null,
                Map.of("punctuation", "，", "splitOnNewline", false, "maxChars", 3)));

        assertEquals(3, result.segments().size());
        assertEquals("甲，", result.segments().get(0).originalText());
        assertEquals("乙，", result.segments().get(1).originalText());
        assertEquals("丙丁戊", result.segments().get(2).originalText());
        assertEquals(4, result.segments().get(2).sourceStart());
    }

    @Test
    void rejectsInvalidRuleValues() {
        BizException exception = assertThrows(BizException.class, () -> provider.analyse(
                new LlmProvider.LlmAnalysisCommand("", "测试", "STRICT", "", null, Map.of("maxChars", 0))));

        assertEquals("LOCAL_RULES_INVALID", exception.getCode());
    }
}
