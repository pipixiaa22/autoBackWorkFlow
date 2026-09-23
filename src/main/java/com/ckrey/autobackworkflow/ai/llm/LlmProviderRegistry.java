package com.ckrey.autobackworkflow.ai.llm;

import com.ckrey.autobackworkflow.common.exception.BizException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class LlmProviderRegistry {
    private final Map<String, LlmProvider> providers;

    public LlmProviderRegistry(List<LlmProvider> providerList) {
        providers = providerList.stream().collect(Collectors.toUnmodifiableMap(
                LlmProvider::providerCode, Function.identity()));
    }

    public LlmProvider getRequired(String providerCode) {
        LlmProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw BizException.badRequest("PROVIDER_NOT_CONFIGURED", "LLM Provider 未配置或未启用");
        }
        return provider;
    }
}
