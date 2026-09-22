package com.ckrey.autobackworkflow.ai.llm;

import com.ckrey.autobackworkflow.ai.model.AnalysisCandidate;
import com.ckrey.autobackworkflow.common.exception.BizException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Safe development fallback: it never invents or rewrites dialogue. */
@Component
public class RuleBasedLlmProvider implements LlmProvider {
    @Override public String providerCode() { return "local-rule"; }
    @Override public AnalysisCandidate analyse(LlmAnalysisCommand command) {
        String source = command.originalDialogue();
        Rules rules = Rules.from(command.localRules());
        List<AnalysisCandidate.CandidateSegment> result = new ArrayList<>();
        int start = 0; int no = 1;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            boolean boundary = rules.punctuation().indexOf(current) >= 0
                    || (rules.splitOnNewline() && current == '\n') || i - start + 1 >= rules.maxChars();
            if (boundary) {
                no = append(source, start, i + 1, no, command.rewriteMode(), result);
                start = i + 1;
            }
        }
        append(source, start, source.length(), no, command.rewriteMode(), result);
        return new AnalysisCandidate(List.copyOf(mergeShortSegments(result, source, rules.minChars())));
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

    private List<AnalysisCandidate.CandidateSegment> mergeShortSegments(
            List<AnalysisCandidate.CandidateSegment> source, String original, int minChars) {
        if (minChars == 0 || source.size() < 2) return source;
        List<AnalysisCandidate.CandidateSegment> merged = new ArrayList<>();
        for (AnalysisCandidate.CandidateSegment current : source) {
            if (!merged.isEmpty() && current.spokenText().length() < minChars) {
                AnalysisCandidate.CandidateSegment previous = merged.removeLast();
                String originalText = original.substring(previous.sourceStart(), current.sourceEnd());
                String text = originalText.trim();
                merged.add(new AnalysisCandidate.CandidateSegment(0, previous.speaker(), previous.sourceStart(),
                        current.sourceEnd(), originalText, text, text, previous.semanticGroup(), previous.emotion(),
                        previous.tone(), previous.speed(), previous.volume(), previous.pauseBeforeMs(),
                        current.pauseAfterMs(), previous.emphasis(), previous.voiceDirection(),
                        previous.rewriteMode(), previous.rewriteLevel(), previous.rewriteReason()));
            } else {
                merged.add(current);
            }
        }
        List<AnalysisCandidate.CandidateSegment> numbered = new ArrayList<>();
        for (int index = 0; index < merged.size(); index++) {
            AnalysisCandidate.CandidateSegment value = merged.get(index);
            numbered.add(new AnalysisCandidate.CandidateSegment(index + 1, value.speaker(), value.sourceStart(),
                    value.sourceEnd(), value.originalText(), value.spokenText(), value.subtitleText(),
                    value.semanticGroup(), value.emotion(), value.tone(), value.speed(), value.volume(),
                    value.pauseBeforeMs(), value.pauseAfterMs(), value.emphasis(), value.voiceDirection(),
                    value.rewriteMode(), value.rewriteLevel(), value.rewriteReason()));
        }
        return numbered;
    }

    private record Rules(int maxChars, String punctuation, boolean splitOnNewline, int minChars) {
        private static final int DEFAULT_MAX_CHARS = 60;
        private static final String DEFAULT_PUNCTUATION = "。！？!?";

        static Rules from(Map<String, Object> values) {
            Map<String, Object> source = values == null ? Map.of() : values;
            int maxChars = integer(source.get("maxChars"), DEFAULT_MAX_CHARS, 1, 500, "maxChars");
            int minChars = integer(source.get("minChars"), 0, 0, maxChars - 1, "minChars");
            String punctuation = source.get("punctuation") instanceof String value && !value.isEmpty()
                    ? value : DEFAULT_PUNCTUATION;
            boolean splitOnNewline = !(source.get("splitOnNewline") instanceof Boolean value) || value;
            return new Rules(maxChars, punctuation, splitOnNewline, minChars);
        }

        private static int integer(Object value, int fallback, int min, int max, String field) {
            if (value == null) return fallback;
            if (!(value instanceof Number number) || number.doubleValue() % 1 != 0
                    || number.intValue() < min || number.intValue() > max) {
                throw BizException.badRequest("LOCAL_RULES_INVALID",
                        field + " 必须是 " + min + " 到 " + max + " 之间的整数");
            }
            return number.intValue();
        }
    }
}
