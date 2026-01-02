package com.acme.herald.auth;

import com.acme.herald.service.WrapAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping(path = "/proxy/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WrapAuthController {
    private final WrapAuthService service;

    public record WrapReq(String token, Integer ttlDays) {
    }

    public record WrapRes(String token, String cookieName, Instant expiresAt) {
    }

    @PostMapping("/wrap")
    public WrapRes wrap(@RequestBody WrapReq req) {
        return service.wrap(req);
    }

    public record LoginPatReq(String username, String pd, Integer ttlDays) {
    }

    @PostMapping(value = "/pat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WrapRes createPat(@RequestBody LoginPatReq req) {
        return service.createPat(req);
    }
}
