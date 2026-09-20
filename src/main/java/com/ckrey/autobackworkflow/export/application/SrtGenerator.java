package com.ckrey.autobackworkflow.export.application;

import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SrtGenerator {
    private final double charsPerSecond; private final int gapMs; private final int commaPauseMs; private final int sentencePauseMs; private final int minDurationMs;
    public SrtGenerator(@Value("${ads.srt.chars-per-second:4.0}") double charsPerSecond, @Value("${ads.srt.gap-ms:200}") int gapMs, @Value("${ads.srt.comma-pause-ms:150}") int commaPauseMs, @Value("${ads.srt.sentence-pause-ms:300}") int sentencePauseMs, @Value("${ads.srt.min-duration-ms:1200}") int minDurationMs) { this.charsPerSecond=charsPerSecond;this.gapMs=gapMs;this.commaPauseMs=commaPauseMs;this.sentencePauseMs=sentencePauseMs;this.minDurationMs=minDurationMs; }
    public String generate(List<AdsDialogueSegment> segments) {
        StringBuilder output=new StringBuilder(); long cursor=0; int index=1;
        for(AdsDialogueSegment segment:segments) { String text=segment.getSubtitleText()==null||segment.getSubtitleText().isBlank()?segment.getSpokenText():segment.getSubtitleText(); long start=cursor+zero(segment.getPauseBeforeMs()); long duration=estimate(text); long end=start+duration; output.append(index++).append('\n').append(timestamp(start)).append(" --> ").append(timestamp(end)).append('\n').append(wrap(text)).append("\n\n"); cursor=end+zero(segment.getPauseAfterMs())+gapMs; }
        return output.toString();
    }
    public long estimate(String text) { long value=Math.round(countChars(text)/charsPerSecond*1000d); for(int i=0;i<text.length();i++) { char c=text.charAt(i); if("，、；：".indexOf(c)>=0)value+=commaPauseMs; if("。！？!?".indexOf(c)>=0)value+=sentencePauseMs; } return Math.max(minDurationMs,value); }
    private static int countChars(String value){ return (int)value.chars().filter(c->!Character.isWhitespace(c)).count(); }
    private static long zero(Integer value){return value==null?0:Math.max(0,value);}
    private static String timestamp(long ms){long h=ms/3_600_000;ms%=3_600_000;long m=ms/60_000;ms%=60_000;long s=ms/1000;return "%02d:%02d:%02d,%03d".formatted(h,m,s,ms%1000);}
    private static String wrap(String value){ List<String> lines=new ArrayList<>(); StringBuilder line=new StringBuilder(); int count=0; for(int i=0;i<value.length();i++){char c=value.charAt(i);line.append(c);if(!Character.isWhitespace(c))count++;if(count>=18&&i<value.length()-1){lines.add(line.toString());line=new StringBuilder();count=0;}}if(!line.isEmpty())lines.add(line.toString());return String.join("\n",lines); }
}
