package com.acme.herald.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm.gpt")
@Getter
@Setter
public class LlmGptProperties {
    private String apiKey;
    private String baseUrl;
}
