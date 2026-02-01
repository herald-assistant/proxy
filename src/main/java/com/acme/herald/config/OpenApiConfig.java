package com.acme.herald.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class OpenApiConfig {
    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("herald")
                .pathsToMatch("/**")
                .addOpenApiCustomizer(openApi -> openApi.setInfo(new Info().title("Herald Proxy API").version("0.1.0")))
                .build();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            String body;
            try {
                body = feign.Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            } catch (IOException e) {
                body = "<failed to read body: " + e.getMessage() + ">";
            }


            log.error("Feign error on {}: status={}, reason={}, body={}",
                    methodKey, response.status(), response.reason(), body);

            return new RuntimeException("Feign error " + response.status() + " for " + methodKey + "with body:" + body);
        };
    }
}
