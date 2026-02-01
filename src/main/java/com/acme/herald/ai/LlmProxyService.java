package com.acme.herald.ai;

import com.acme.herald.auth.CryptoService;
import com.acme.herald.auth.MeService;
import com.acme.herald.config.LlmConfigService;
import com.acme.herald.config.LlmIntegrationDtos.StoredCatalog;
import com.acme.herald.config.LlmIntegrationDtos.StoredGitHubCopilot;
import com.acme.herald.config.LlmIntegrationDtos.StoredModel;
import com.acme.herald.domain.ChatDtos;
import com.acme.herald.web.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmProxyService {

    private final RestClient rest;
    private final LlmConfigService llmConfig;
    private final MeService meService;
    private final CryptoService crypto;
    private final JsonMapper jsonMapper;

    public ChatDtos.ProxyReply chat(ChatDtos.ChatRequest req) {
        ChatDtos.ChatResponse res = chatRaw(req);

        String reply = "";
        String finish = null;
        if (res.choices() != null && !res.choices().isEmpty()) {
            var choice = res.choices().getFirst();
            reply = choice.message() != null ? choice.message().content() : "";
            finish = choice.finish_reason();
        }

        Map<String, Object> raw = new HashMap<>();
        raw.put("id", res.id());
        raw.put("model", res.model());
        raw.put("created", res.created());
        raw.put("choicesCount", res.choices() != null ? res.choices().size() : 0);

        return new ChatDtos.ProxyReply(reply, finish, res.usage(), raw);
    }

    /**
     * Zwraca pełny ChatResponse upstream.
     * req.model() = modelId z katalogu admina (np. "openai_gpt4o" / "copilot_gpt4o").
     *
     * Token NIE przychodzi z FE — jest dobierany po stronie proxy:
     * - zwykłe modele: tokenEnc z katalogu (project property)
     * - Copilot: user token z profilu lub global PAT z katalogu
     */
    public ChatDtos.ChatResponse chatRaw(ChatDtos.ChatRequest req) {
        StoredCatalog stored = llmConfig.getStoredForRuntime(); // includes encrypted secrets
        StoredModel cfg = findEnabledModelOrThrow(stored, req.model());

        String upstreamModelName = (cfg.model() == null ? "" : cfg.model().trim());
        if (upstreamModelName.isEmpty()) {
            throw new IllegalArgumentException("Model '%s' nie ma ustawionego pola model (upstream)".formatted(cfg.id()));
        }

        String url = resolveChatCompletionsUrl(cfg);

        String bearer = resolveBearerToken(stored, cfg);

        Double temperature = req.temperature() != null ? req.temperature()
                : (cfg.defaults() != null ? cfg.defaults().temperature() : null);
        if (temperature == null) temperature = 0.2;

        Integer maxTokens = req.max_tokens() != null ? req.max_tokens()
                : (cfg.defaults() != null ? cfg.defaults().maxTokens() : null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", upstreamModelName);
        payload.put("messages", req.messages());
        payload.put("stream", false);

        if (temperature != null) payload.put("temperature", temperature);
        if (maxTokens != null) payload.put("max_tokens", maxTokens);

        boolean appliedMaxTokensFix = false;
        boolean removedTemperature = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return doPost(cfg, url, bearer, payload);
            } catch (UpstreamException e) {
                if (e.status() != 400) throw e;

                String unsupportedParam = detectUnsupportedParam(e.body());

                if (!appliedMaxTokensFix
                        && "max_tokens".equals(unsupportedParam)
                        && payload.containsKey("max_tokens")) {

                    Object mt = payload.remove("max_tokens");
                    if (mt != null) payload.put("max_completion_tokens", mt);

                    appliedMaxTokensFix = true;
                    continue;
                }

                if (!removedTemperature
                        && "temperature".equals(unsupportedParam)
                        && payload.containsKey("temperature")) {

                    payload.remove("temperature");
                    removedTemperature = true;
                    continue;
                }

                throw e;
            }
        }

        throw new IllegalStateException("LLM proxy retry loop ended unexpectedly for modelId=" + cfg.id());
    }

    // ─────────────────────────────────────────────────────────────────
    // Token resolution
    // ─────────────────────────────────────────────────────────────────

    /**
     * Reguły tokena:
     * - jeśli model.githubCopilotModel=true:
     *    - jeśli stored.githubCopilot.useUserToken=true -> token z profilu użytkownika (issue property)
     *    - else -> global PAT z katalogu (stored.githubCopilot.patEnc)
     * - else -> token modelu z katalogu (cfg.tokenEnc)
     */
    private String resolveBearerToken(StoredCatalog stored, StoredModel cfg) {
        boolean isCopilotModel = Boolean.TRUE.equals(cfg.githubCopilotModel());

        if (isCopilotModel) {
            StoredGitHubCopilot cop = stored.githubCopilot();
            boolean useUserToken = cop != null && Boolean.TRUE.equals(cop.useUserToken());

            if (useUserToken) {
                String userToken = meService.getMyGithubCopilotTokenOrNull();
                if (userToken == null || userToken.isBlank()) {
                    throw new UnauthorizedException(
                            "Brak tokena GitHub Copilot w profilu użytkownika. Ustaw go w 'Mój profil' (GitHub Copilot token)."
                    );
                }
                return userToken.trim(); // już plaintext
            }

            // global PAT
            String patEnc = (cop != null) ? cop.patEnc() : null;
            if (patEnc == null || patEnc.isBlank()) {
                throw new IllegalStateException("Copilot: useUserToken=false, ale brak globalnego PAT w konfiguracji admina.");
            }
            return decryptSecret(patEnc);
        }

        // zwykły model: tokenEnc z katalogu
        String tokenEnc = cfg.tokenEnc();
        if (tokenEnc == null || tokenEnc.isBlank()) {
            throw new IllegalStateException("Model '%s' nie ma ustawionego tokena w konfiguracji admina.".formatted(cfg.id()));
        }
        return decryptSecret(tokenEnc);
    }

    private String decryptSecret(String enc) {
        byte[] plain = crypto.decrypt(enc);
        String token = new String(plain, StandardCharsets.UTF_8).trim();
        if (token.isEmpty()) throw new IllegalStateException("Zdekryptowany token jest pusty.");
        return token;
    }

    private StoredModel findEnabledModelOrThrow(StoredCatalog stored, String modelId) {
        String id = (modelId == null ? "" : modelId.trim());
        if (id.isEmpty()) throw new IllegalArgumentException("Brak modelId w request.model");

        List<StoredModel> models = stored.models() != null ? stored.models() : List.of();

        return models.stream()
                .filter(m -> id.equals((m.id() == null ? "" : m.id()).trim()))
                .filter(m -> Boolean.TRUE.equals(m.enabled()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Model '%s' nie istnieje lub jest wyłączony".formatted(id)));
    }

    // ─────────────────────────────────────────────────────────────────
    // HTTP call
    // ─────────────────────────────────────────────────────────────────

    private ChatDtos.ChatResponse doPost(StoredModel cfg, String url, String bearer, Map<String, Object> payload) {
        return rest.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + bearer)
                .body(payload)
                .retrieve()
                .onStatus(status -> status.isError(), (request, response) -> {
                    throw toUpstreamException(cfg, response);
                })
                .body(ChatDtos.ChatResponse.class);
    }

    private String detectUnsupportedParam(String exceptionBody) {
        if (exceptionBody == null || exceptionBody.isBlank()) return null;

        try {
            JsonNode root = jsonMapper.readValue(exceptionBody, JsonNode.class);
            JsonNode err = root.path("error");
            String code = err.path("code").asString("");
            String param = err.path("param").asString("");
            if ("unsupported_parameter".equals(code) && !param.isBlank()) return param;
        } catch (Exception ignored) {}

        if (exceptionBody.contains("'max_tokens'")) return "max_tokens";
        if (exceptionBody.contains("'temperature'")) return "temperature";
        return null;
    }

    private String resolveChatCompletionsUrl(StoredModel cfg) {
        String base = cfg.baseUrl() != null ? cfg.baseUrl().trim() : "";
        if (base.isEmpty()) {
            throw new IllegalArgumentException("Model '%s' nie ma ustawionego baseUrl".formatted(cfg.id()));
        }

        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.contains("/chat/completions")) return base;

        return base + "/chat/completions";
    }

    private UpstreamException toUpstreamException(StoredModel cfg, ClientHttpResponse response) {
        int status = safeStatus(response);
        String body = safeBody(response);

        String msg = "LLM upstream error: modelId=%s status=%d body=%s"
                .formatted(cfg.id(), status, body);

        return new UpstreamException(msg, cfg.id(), status, body);
    }

    private String safeBody(ClientHttpResponse response) {
        try (InputStream is = response.getBody()) {
            byte[] bytes = is != null ? is.readAllBytes() : new byte[0];
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(failed to read body: %s)".formatted(e.getMessage());
        }
    }

    private int safeStatus(ClientHttpResponse response) {
        try {
            return response.getStatusCode().value();
        } catch (Exception e) {
            return -1;
        }
    }

    static final class UpstreamException extends RuntimeException {
        private final String modelId;
        private final int status;
        private final String body;

        UpstreamException(String message, String modelId, int status, String body) {
            super(message);
            this.modelId = modelId;
            this.status = status;
            this.body = body;
        }

        public String modelId() { return modelId; }
        public int status() { return status; }
        public String body() { return body; }
    }
}
