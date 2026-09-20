package com.ckrey.autobackworkflow.config;

import com.ckrey.autobackworkflow.ai.config.DeepSeekProperties;
import com.ckrey.autobackworkflow.audio.config.SeedAudioProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DeepSeekProperties.class, SeedAudioProperties.class})
public class AiProviderConfiguration { }
