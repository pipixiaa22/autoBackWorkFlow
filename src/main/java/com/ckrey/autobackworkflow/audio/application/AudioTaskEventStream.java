package com.ckrey.autobackworkflow.audio.application;

import com.ckrey.autobackworkflow.domain.AdsGenerationTask;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class AudioTaskEventStream {
    private static final Set<String> TERMINAL = Set.of("SUCCEEDED", "PARTIAL_SUCCESS", "FAILED", "CANCELLED");
    private final Map<Long, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("audio-sse-heartbeat").factory());

    public AudioTaskEventStream() {
        heartbeat.scheduleAtFixedRate(this::sendHeartbeats, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void close() {
        heartbeat.shutdownNow();
    }

    public SseEmitter subscribe(Long taskId, Supplier<Map<String, Object>> snapshot) {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.computeIfAbsent(taskId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(taskId, emitter));
        emitter.onTimeout(() -> remove(taskId, emitter));
        emitter.onError(error -> remove(taskId, emitter));
        try {
            send(taskId, emitter, snapshot.get());
        } catch (RuntimeException ex) {
            remove(taskId, emitter);
            throw ex;
        }
        return emitter;
    }

    public boolean hasSubscribers(Long taskId) {
        return subscribers.containsKey(taskId);
    }

    public void publish(Long taskId, Map<String, Object> snapshot) {
        Set<SseEmitter> listeners = subscribers.get(taskId);
        if (listeners == null) return;
        for (SseEmitter emitter : listeners) send(taskId, emitter, snapshot);
    }

    private void send(Long taskId, SseEmitter emitter, Map<String, Object> snapshot) {
        AdsGenerationTask task = (AdsGenerationTask) snapshot.get("task");
        boolean complete = TERMINAL.contains(task.getStatus());
        try {
            emitter.send(SseEmitter.event().name(complete ? "complete" : "task").data(snapshot));
            if (complete) {
                remove(taskId, emitter);
                emitter.complete();
            }
        } catch (IOException | IllegalStateException ex) {
            remove(taskId, emitter);
            emitter.completeWithError(ex);
        }
    }

    private void remove(Long taskId, SseEmitter emitter) {
        Set<SseEmitter> listeners = subscribers.get(taskId);
        if (listeners == null) return;
        listeners.remove(emitter);
        if (listeners.isEmpty()) subscribers.remove(taskId, listeners);
    }

    private void sendHeartbeats() {
        subscribers.forEach((taskId, listeners) -> {
            for (SseEmitter emitter : listeners) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException | IllegalStateException ex) {
                    remove(taskId, emitter);
                    emitter.completeWithError(ex);
                }
            }
        });
    }
}
