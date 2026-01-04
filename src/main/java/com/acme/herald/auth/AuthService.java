package com.acme.herald.auth;

import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final HeraldAuthProps props;
    private final CryptoService crypto;
    private final JiraProvider jira;
    private final JsonMapper jsonMapper;

    public AuthDtos.WrapRes wrap(AuthDtos.WrapReq req) {
        int days = clampDays(req.ttlDays(), props.getMaxAgeDays());
        var payload = new TokenPayload(req.token(), Instant.now().plus(Duration.ofDays(days)), null);

        return encrypt(payload);
    }

    public AuthDtos.WrapRes createPat(AuthDtos.LoginPatReq req) {
        int max = Math.min(props.getMaxAgeDays(), 30);
        int days = clampDays(req.ttlDays(), max);

        TokenPayload payload = jira.createPatByUsernamePdWithMeta(req.username(), req.pd(), days);

        return encrypt(payload);
    }

    public void revokeCurrentPat() {
        jira.revokeCurrentPat();
    }

    private AuthDtos.WrapRes encrypt(TokenPayload payload) {
        try {
            byte[] json = jsonMapper.writeValueAsBytes(payload);
            String enc = crypto.encrypt(json);
            return new AuthDtos.WrapRes(enc, props.getCookieName(), payload.exp());
        } catch (Exception e) {
            throw new IllegalStateException("Token wrapping failed.", e);
        }
    }

    private static int clampDays(Integer requested, int maxDays) {
        int d = (requested == null || requested <= 0) ? maxDays : requested;
        if (d < 1) d = 1;
        if (d > maxDays) d = maxDays;
        return d;
    }
}
