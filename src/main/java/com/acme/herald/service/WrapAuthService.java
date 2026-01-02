package com.acme.herald.service;

import com.acme.herald.auth.CryptoService;
import com.acme.herald.auth.HeraldAuthProps;
import com.acme.herald.auth.TokenPayload;
import com.acme.herald.auth.WrapAuthController;
import com.acme.herald.provider.server.JiraServerProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WrapAuthService {
    private final HeraldAuthProps props;
    private final CryptoService crypto;
    private final JiraServerProvider jira;

    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public WrapAuthController.WrapRes wrap(WrapAuthController.WrapReq req) {
        int days = clampDays(req.ttlDays(), props.getMaxAgeDays());
        var payload = new TokenPayload(req.token(), Instant.now().plus(Duration.ofDays(days)));
        try {
            byte[] json = om.writeValueAsBytes(payload);
            String enc = crypto.encrypt(json);
            return new WrapAuthController.WrapRes(enc, props.getCookieName(), payload.exp());
        } catch (Exception e) {
            throw new IllegalStateException("wrap failed", e);
        }
    }

    public WrapAuthController.WrapRes createPat(WrapAuthController.LoginPatReq req) {
        int max = Math.min(props.getMaxAgeDays(), 30);
        int days = clampDays(req.ttlDays(), max);

        String rawPat = jira.createPatByUsernamePd(req.username(), req.pd(), days);

        return wrap(new WrapAuthController.WrapReq(rawPat, days));
    }

    private static int clampDays(Integer requested, int maxDays) {
        int d = (requested == null || requested <= 0) ? maxDays : requested;
        if (d < 1) d = 1;
        if (d > maxDays) d = maxDays;
        return d;
    }
}
