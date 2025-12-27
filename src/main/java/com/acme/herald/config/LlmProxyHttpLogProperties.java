package com.acme.herald.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "herald.llm.proxy.http-log")
public class LlmProxyHttpLogProperties {
    private boolean enabled = false;
    private boolean logBodies = false;
    private int maxBodyChars = 8000;
    private boolean logHeaders = true;
    private boolean onlyErrors = false;
}
