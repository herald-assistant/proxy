package com.acme.herald.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;

public class OpenAiFeignConfig {

    @Bean
    RequestInterceptor openAiAuthInterceptor(LlmGptProperties props) {
        return template -> {
            template.header("Authorization", "Bearer " + props.getApiKey());
            template.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            // Jeśli używasz organizacji: template.header("OpenAI-Organization", "org_...");
        };
    }

    @Bean
    ErrorDecoder openAiErrorDecoder() {
        return (methodKey, response) -> new RuntimeException(
                "OpenAI error %d %s".formatted(response.status(), response.reason()));
    }
}
