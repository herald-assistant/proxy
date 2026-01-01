package com.acme.herald.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "herald.auth")
public class HeraldAuthProps {
    private String cookieName;
    private int maxAgeDays;
    private String secretB64;
    private List<String> allowedOrigins;
}
