package com.ckrey.autobackworkflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule longAsString = new SimpleModule();
        longAsString.addSerializer(Long.class, ToStringSerializer.instance);
        longAsString.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return new ObjectMapper().registerModule(longAsString);
    }

    /**
     * Spring Boot 4 serializes MVC responses with Jackson 3, while application code above uses
     * Jackson 2. Configure both mappers so HTTP responses never expose unsafe JSON numbers.
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            tools.jackson.databind.module.SimpleModule longAsString =
                    new tools.jackson.databind.module.SimpleModule();
            longAsString.addSerializer(Long.class, tools.jackson.databind.ser.std.ToStringSerializer.instance);
            longAsString.addSerializer(Long.TYPE, tools.jackson.databind.ser.std.ToStringSerializer.instance);
            builder.addModule(longAsString);
        };
    }
}
