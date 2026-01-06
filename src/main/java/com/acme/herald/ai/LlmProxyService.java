package com.acme.herald.ai;

import com.acme.herald.auth.CryptoService;
import com.acme.herald.domain.ChatDtos;
import com.acme.herald.config.LlmIntegrationDtos.*;
import com.acme.herald.config.LlmConfigService;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmProxyService {

    private final RestClient rest;
    private final LlmConfigService llmConfig;
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
     * req.model() = modelId z katalogu admina (np. "openai_gpt4o").
     */
    public ChatDtos.ChatResponse chatRaw(ChatDtos.ChatRequest req) {
        LlmCatalogModelDto cfg = llmConfig.findEnabledModelInternalOrThrow(req.model());

        String upstreamModelName = (cfg.model() == null ? "" : cfg.model().trim());
        if (upstreamModelName.isEmpty()) {
            throw new IllegalArgumentException("Model '%s' nie ma ustawionego pola model (upstream)".formatted(cfg.id()));
        }

        String url = resolveChatCompletionsUrl(cfg);
        String bearer = decryptBearerOrThrow(cfg);

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

        // max 3 próby: (1) normal, (2) po pierwszym fallbacku, (3) po drugim fallbacku
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return doPost(cfg, url, bearer, payload);
            } catch (UpstreamException e) {
                // fallbacki tylko dla 400
                if (e.status() != 400) throw e;

                String unsupportedParam = detectUnsupportedParam(e.body());

                // Fallback #1: max_tokens -> max_completion_tokens
                if (!appliedMaxTokensFix
                        && "max_tokens".equals(unsupportedParam)
                        && payload.containsKey("max_tokens")) {

                    Object mt = payload.remove("max_tokens");
                    if (mt != null) payload.put("max_completion_tokens", mt);

                    appliedMaxTokensFix = true;
                    continue; // retry
                }

                // Fallback #2: usuń temperature
                if (!removedTemperature
                        && "temperature".equals(unsupportedParam)
                        && payload.containsKey("temperature")) {

                    payload.remove("temperature");
                    removedTemperature = true;
                    continue; // retry
                }

                // Jeśli nie pasuje do fallbacków -> propaguj błąd
                throw e;
            }
        }

        // Teoretycznie nieosiągalne (bo pętla zawsze return/throw), ale dla spokoju kompilatora:
        throw new IllegalStateException("LLM proxy retry loop ended unexpectedly for modelId=" + cfg.id());
    }

    private ChatDtos.ChatResponse doPost(LlmCatalogModelDto cfg, String url, String bearer, Map<String, Object> payload) {
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

    /**
     * Próbuje wyciągnąć error.param z body w stylu OpenAI:
     * { "error": { "code":"unsupported_parameter", "param":"temperature", ... } }
     * Jak się nie da, robi prosty fallback string-match.
     */
    private String detectUnsupportedParam(String exceptionBody) {
        if (exceptionBody == null || exceptionBody.isBlank()) return null;

        try {
            JsonNode root = jsonMapper.readValue(exceptionBody, JsonNode.class);
            JsonNode err = root.path("error");
            String code = err.path("code").asString("");
            String param = err.path("param").asString("");
            if ("unsupported_parameter".equals(code) && !param.isBlank()) {
                return param;
            }
        } catch (Exception ignored) {
            // ignore
        }

        // string fallback (tani i odporny)
        if (exceptionBody.contains("'max_tokens'")) return "max_tokens";
        if (exceptionBody.contains("'temperature'")) return "temperature";

        return null;
    }

    private String decryptBearerOrThrow(LlmCatalogModelDto cfg) {
        String enc = cfg.token();
        boolean present = Boolean.TRUE.equals(cfg.tokenPresent());

        if ((enc == null || enc.isBlank()) && present) {
            throw new IllegalStateException("Model '%s' ma tokenSet=true, ale brak token w konfiguracji".formatted(cfg.id()));
        }
        if (enc == null || enc.isBlank()) {
            throw new IllegalArgumentException("Model '%s' nie ma ustawionego tokena".formatted(cfg.id()));
        }

        byte[] plain = crypto.decrypt(enc);
        return new String(plain, StandardCharsets.UTF_8).trim();
    }

    private String resolveChatCompletionsUrl(LlmCatalogModelDto cfg) {
        String base = cfg.baseUrl() != null ? cfg.baseUrl().trim() : "";
        if (base.isEmpty()) {
            throw new IllegalArgumentException("Model '%s' nie ma ustawionego baseUrl".formatted(cfg.id()));
        }

        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.contains("/chat/completions")) return base;

        return base + "/chat/completions";
    }

    private UpstreamException toUpstreamException(LlmCatalogModelDto cfg, ClientHttpResponse response) {
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

    /**
     * Własny wyjątek z status+body, żeby robić fallbacki.
     */
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

        public String modelId() {
            return modelId;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }
    }
}
