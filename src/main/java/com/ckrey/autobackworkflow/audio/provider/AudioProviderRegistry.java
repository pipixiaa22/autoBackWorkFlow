package com.ckrey.autobackworkflow.audio.provider;

import com.ckrey.autobackworkflow.common.exception.BizException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class AudioProviderRegistry {
    private final Map<String, AudioGenerationProvider> providers;

    public AudioProviderRegistry(List<AudioGenerationProvider> providerList) {
        providers = providerList.stream().collect(Collectors.toUnmodifiableMap(AudioGenerationProvider::providerCode, Function.identity()));
    }

    public AudioGenerationProvider getRequired(String code) {
        AudioGenerationProvider p = providers.get(code);
        if (p == null) throw BizException.badRequest("PROVIDER_NOT_CONFIGURED", "音频 Provider 未配置或未启用");
        return p;
    }
}
