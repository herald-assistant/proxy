package com.acme.herald.auth;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StatelessAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_CURRENT_AUTH = "herald.currentAuth";

    private final HeraldAuthProps props;
    private final CryptoService crypto;
    private final JsonMapper jsonMapper;


    private final JiraConfigService jiraCfg;
    private final JiraProvider jira;

    public StatelessAuthFilter(
            HeraldAuthProps props,
            CryptoService crypto,
            JsonMapper jsonMapper,
            JiraConfigService jiraCfg,
            JiraProvider jira
    ) {
        this.props = props;
        this.crypto = crypto;
        this.jsonMapper = jsonMapper;
        this.jiraCfg = jiraCfg;
        this.jira = jira;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws java.io.IOException, jakarta.servlet.ServletException {

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

        if (enc == null || enc.isBlank()) {
            send401(res, "NO_TOKEN");
            return;
        }

        TokenPayload tp;
        try {
            tp = jsonMapper.readValue(crypto.decrypt(enc), TokenPayload.class);
        } catch (Exception ex) {
            send401(res, "TOKEN_INVALID");
            return;
        }

        if (tp.exp() == null || tp.exp().isBefore(Instant.now())) {
            send401(res, "TOKEN_EXPIRED");
            return;
        }

        req.setAttribute(ATTR_CURRENT_AUTH, tp);

        if (!shouldNotAuthorize(req)) {
            AccessDecision decision = checkAccess();
            if (!decision.allowed) {
                send403(res, decision.reason);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private boolean shouldNotAuthorize(HttpServletRequest req) {
        String p = req.getRequestURI();
        return shouldNotFilter(req)
                || p.contains("/admin/")
                || p.contains("/me/");
    }

    private AccessDecision checkAccess() {
        try {
            var cfg = jiraCfg.getForRuntime();
            var access = (cfg != null) ? cfg.access() : null;

            List<String> allow = access != null && access.allowGroups() != null ? access.allowGroups() : List.of();
            List<String> deny = access != null && access.denyGroups() != null ? access.denyGroups() : List.of();

            // jeśli nic nie skonfigurowano -> system "otwarty"
            if (allow.isEmpty() && deny.isEmpty()) {
                return AccessDecision.allow();
            }

            Set<String> userGroups = getUserGroups();

            // konserwatywnie: jeśli nie umiemy pobrać grup, a access jest ustawiony -> blokuj
            if (userGroups.isEmpty()) {
                return AccessDecision.deny("ACCESS_NO_GROUPS_RESOLVED");
            }

            // deny ma priorytet
            boolean inDeny = deny.stream().anyMatch(userGroups::contains);
            if (inDeny) {
                return AccessDecision.deny("ACCESS_DENIED_BY_GROUP");
            }

            // allow: jeśli lista pusta => każdy (o ile nie w deny)
            if (allow.isEmpty()) {
                return AccessDecision.allow();
            }

            boolean inAllow = allow.stream().anyMatch(userGroups::contains);
            if (!inAllow) {
                return AccessDecision.deny("ACCESS_NOT_IN_ALLOW_GROUPS");
            }

            return AccessDecision.allow();
        } catch (Exception e) {
            // jeśli access skonfigurowany, ale check się wysypał -> bezpieczniej deny
            return AccessDecision.deny("ACCESS_CHECK_FAILED");
        }
    }

    private void send401(HttpServletResponse res, String reason) throws java.io.IOException {
        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("{\"reason\":\"" + reason + "\"}");
    }

    private void send403(HttpServletResponse res, String reason) throws java.io.IOException {
        res.setStatus(403);
        res.setContentType("application/json");
        res.getWriter().write("{\"reason\":\"" + reason + "\"}");
    }

    private Set<String> getUserGroups() {
        return jira.getMe().groups().items()
                .stream()
                .map(JiraModels.GroupItem::name)
                .collect(Collectors.toSet());
    }

    private static final class AccessDecision {
        final boolean allowed;
        final String reason;

        private AccessDecision(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        static AccessDecision allow() {
            return new AccessDecision(true, "OK");
        }

        static AccessDecision deny(String reason) {
            return new AccessDecision(false, reason);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        String p = req.getRequestURI();
        return p.startsWith("/proxy/auth/wrap") || p.endsWith("/proxy/auth/wrap")
                || p.startsWith("/proxy/auth/pat") || p.endsWith("/proxy/auth/pat")
                || p.startsWith("/actuator") || p.endsWith("/actuator")
                || p.startsWith("/v3/api-docs") || p.endsWith("/v3/api-docs");
    }
}
