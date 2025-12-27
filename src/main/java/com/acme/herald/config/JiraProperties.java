package com.acme.herald.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jira")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JiraProperties {
    private String baseUrl;
    private String projectKey;
    private String apiVersion; // "2" or "3"
}