package com.ckrey.autobackworkflow.ai.llm;

import com.ckrey.autobackworkflow.ai.model.AnalysisCandidate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Safe development fallback: it never invents or rewrites dialogue. */
@Component
public class RuleBasedLlmProvider implements LlmProvider {
    @Override public String providerCode() { return "local-rule"; }
    @Override public AnalysisCandidate analyse(LlmAnalysisCommand command) {
        String source = command.originalDialogue();
        List<AnalysisCandidate.CandidateSegment> result = new ArrayList<>();
        int start = 0; int no = 1;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            boolean boundary = "。！？!?\n".indexOf(current) >= 0 || i - start + 1 >= 60;
            if (boundary) {
                no = append(source, start, i + 1, no, command.rewriteMode(), result);
                start = i + 1;
            }
        }
        append(source, start, source.length(), no, command.rewriteMode(), result);
        return new AnalysisCandidate(List.copyOf(result));
    }
    private int append(String source, int rawStart, int rawEnd, int no, String rewriteMode,
                       List<AnalysisCandidate.CandidateSegment> target) {
        int start = rawStart, end = rawEnd;
        while (start < end && Character.isWhitespace(source.charAt(start))) start++;
        while (end > start && Character.isWhitespace(source.charAt(end - 1))) end--;
        if (start == end) return no;
        String text = source.substring(start, end);
        target.add(new AnalysisCandidate.CandidateSegment(no, "未指定", start, end, text, text, text,
                "未指定", new AnalysisCandidate.Emotion("neutral", null, new BigDecimal("0.50")),
                List.of("natural"), BigDecimal.ONE, BigDecimal.ONE, 0, 350, List.of(),
                "自然表达，保持原句含义。", rewriteMode, "NONE", "未改写原始台词"));
        return no + 1;
    }
}
