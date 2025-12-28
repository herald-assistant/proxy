package com.acme.herald.service;

import com.acme.herald.auth.CryptoService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.admin.LlmIntegrationDtos.*;
import com.acme.herald.web.error.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminLlmConfigService {

    private static final String PROP_KEY = "herald.llmCatalog";
    private static final String PERM_ADMIN = "ADMINISTER_PROJECTS";

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final ObjectMapper om  = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final CryptoService crypto;

    public LlmCatalogDto getCatalog() {
        requireProjectAdmin();

        StoredCatalog stored = loadStored();

        List<LlmCatalogModelDto> models = new ArrayList<>();
        for (StoredModel m : stored.models()) {
            models.add(new LlmCatalogModelDto(
                    m.id(),
                    m.label(),
                    m.model(),
                    m.baseUrl(),
                    m.contextWindowTokens(),
                    m.enabled(),
                    m.notes(),
                    m.supports(),
                    m.defaults(),
                    null, // token write-only
                    m.tokenEnc() != null && !m.tokenEnc().isBlank()
            ));
        }
        return new LlmCatalogDto(stored.version(), models);
    }

    public void upsertLlmCatalog(LlmCatalogDto incoming) {
        requireProjectAdmin(); // 👈 autoryzacja

        StoredCatalog current = loadStored();
        Map<String, StoredModel> byId = new HashMap<>();
        for (StoredModel m : current.models()) byId.put(m.id(), m);

        List<StoredModel> out = new ArrayList<>();
        for (LlmCatalogModelDto m : Optional.ofNullable(incoming.models()).orElse(List.of())) {
            if (m.id() == null || m.id().isBlank()) continue;

            StoredModel prev = byId.get(m.id());
            String tokenEnc = (prev != null) ? prev.tokenEnc() : null;

            // token write-only: jeśli przyszedł -> nadpisz, jeśli nie -> zostaw
            String tokenPlain = m.token();
            if (tokenPlain != null && !tokenPlain.trim().isEmpty()) {
                tokenEnc = crypto.encrypt(tokenPlain.trim().getBytes(StandardCharsets.UTF_8));
            }

            out.add(new StoredModel(
                    m.id().trim(),
                    nz(m.label()),
                    nz(m.model()),
                    blankToNull(m.baseUrl()),
                    m.contextWindowTokens(),
                    m.enabled(),
                    blankToNull(m.notes()),
                    m.supports(),
                    m.defaults(),
                    tokenEnc
            ));
        }

        StoredCatalog stored = new StoredCatalog(
                incoming.version() != null ? incoming.version() : 1,
                out
        );

        saveStored(stored);
    }

    public LlmCatalogModelDto findEnabledModelInternalOrThrow(String modelId) {
        var id = (modelId == null ? "" : modelId.trim());
        if (id.isEmpty()) throw new IllegalArgumentException("Brak modelId w request.model");

        var cat = getCatalogInternal();
        var models = cat.models() != null ? cat.models() : List.<LlmCatalogModelDto>of();

        return models.stream()
                .filter(m -> id.equals((m.id()).trim()))
                .filter(m -> Boolean.TRUE.equals(m.enabled()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Model '%s' nie istnieje lub jest wyłączony".formatted(id)));
    }

    public LlmCatalogDto getCatalogInternal() {
        requireProjectAdmin();

        StoredCatalog stored = loadStored();

        List<LlmCatalogModelDto> models = new ArrayList<>();
        for (StoredModel m : stored.models()) {
            models.add(new LlmCatalogModelDto(
                    m.id(),
                    m.label(),
                    m.model(),
                    m.baseUrl(),
                    m.contextWindowTokens(),
                    m.enabled(),
                    m.notes(),
                    m.supports(),
                    m.defaults(),
                    m.tokenEnc(),
                    m.tokenEnc() != null && !m.tokenEnc().isBlank()
            ));
        }
        return new LlmCatalogDto(stored.version(), models);
    }


    // ─────────────────────────────────────────────────────────────────

    private void requireProjectAdmin() {
        var projectKey = jiraProps.getProjectKey();
        JiraModels.PermissionsResponse perms = jira.getMyPermissions(projectKey, null, null);
        var map = perms != null ? perms.permissions() : null;

        boolean isAdmin = map != null
                && map.get(PERM_ADMIN) != null
                && Boolean.TRUE.equals(map.get(PERM_ADMIN).havePermission());

        if (!isAdmin) {
            throw new ForbiddenException("Brak uprawnienia: " + PERM_ADMIN + " w projekcie " + projectKey);
        }
    }

    private StoredCatalog loadStored() {
        Map<String, Object> resp = jira.getProjectProperty(jiraProps.getProjectKey(), PROP_KEY);
        if (resp == null || !resp.containsKey("value")) {
            return new StoredCatalog(1, new ArrayList<>());
        }
        Object value = resp.get("value");
        return om.convertValue(value, StoredCatalog.class);
    }

    private void saveStored(StoredCatalog stored) {
        jira.setProjectProperty(jiraProps.getProjectKey(), PROP_KEY, stored);
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
