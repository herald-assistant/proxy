package com.acme.herald.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Instant;
import java.util.Arrays;

public class StatelessAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_CURRENT_AUTH = "herald.currentAuth";

    private final HeraldAuthProps props;
    private final CryptoService crypto;

    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public StatelessAuthFilter(HeraldAuthProps props, CryptoService crypto) {
        this.props = props;
        this.crypto = crypto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return p.startsWith("/proxy/auth/wrap") || p.endsWith("/proxy/auth/wrap")
                || p.startsWith("/proxy/auth/pat") || p.endsWith("/proxy/auth/pat")
                || p.startsWith("/actuator") || p.endsWith("/actuator")
                || p.startsWith("/v3/api-docs") || p.endsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws java.io.IOException, jakarta.servlet.ServletException {

        // Preferencja: header -> cookie
        String enc = req.getHeader("X-Herald-Auth");
        if (enc == null || enc.isBlank()) {
            Cookie[] cookies = req.getCookies();
            Cookie c = cookies == null ? null :
                    Arrays.stream(cookies)
                            .filter(x -> props.getCookieName().equals(x.getName()))
                            .findFirst()
                            .orElse(null);
            enc = c == null ? null : c.getValue();
        }

        if (enc == null || enc.isBlank()) { send401(res, "NO_TOKEN"); return; }

        TokenPayload tp;
        try {
            tp = om.readValue(crypto.decrypt(enc), TokenPayload.class);
        } catch (Exception ex) {
            send401(res, "TOKEN_INVALID");
            return;
        }

        if (tp.exp() == null || tp.exp().isBefore(Instant.now())) { send401(res, "TOKEN_EXPIRED"); return; }

        req.setAttribute(ATTR_CURRENT_AUTH, tp);
        chain.doFilter(req, res);
    }

    private void send401(HttpServletResponse res, String reason) throws java.io.IOException {
        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("{\"reason\":\"" + reason + "\"}");
    }
}
