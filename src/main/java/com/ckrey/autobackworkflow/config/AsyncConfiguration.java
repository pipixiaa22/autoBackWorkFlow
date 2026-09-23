package com.ckrey.autobackworkflow.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncConfiguration {
    @Bean(destroyMethod = "close")
    public Executor applicationTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
