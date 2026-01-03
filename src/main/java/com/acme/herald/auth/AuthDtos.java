package com.acme.herald.auth;

import java.time.Instant;

public class AuthDtos {
    public record WrapReq(String token, Integer ttlDays) {
    }

    public record WrapRes(String token, String cookieName, Instant expiresAt) {
    }

    public record LoginPatReq(String username, String pd, Integer ttlDays) {
    }

}
