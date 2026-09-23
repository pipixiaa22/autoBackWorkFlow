package com.ckrey.autobackworkflow.audio.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ads.ai.seed-audio")
public class SeedAudioProperties {
    private String baseUrl = "https://openspeech.bytedance.com";
    private String apiKey = "";
    private String model = "seed-audio-1.0";
    private int connectTimeoutMs = 10_000;
    private int requestTimeoutMs = 180_000;
    private int maxAttempts = 1;
    private List<String> referenceUrlAllowedHosts = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public List<String> getReferenceUrlAllowedHosts() {
        return referenceUrlAllowedHosts;
    }

    public void setReferenceUrlAllowedHosts(List<String> referenceUrlAllowedHosts) {
        this.referenceUrlAllowedHosts = referenceUrlAllowedHosts == null ? new ArrayList<>() : referenceUrlAllowedHosts;
    }
}
