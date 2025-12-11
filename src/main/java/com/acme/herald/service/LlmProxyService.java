package com.acme.herald.service;

import com.acme.herald.domain.ChatDtos;
import com.acme.herald.provider.feign.OpenAiClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LlmProxyService {

    private final OpenAiClient client;

    public LlmProxyService(OpenAiClient client) {
        this.client = client;
    }

    public ChatDtos.ProxyReply chat(ChatDtos.ChatRequest req) {
        // Domyślne bezpieczeństwo: wymuszamy brak stream po stronie proxy
        ChatDtos.ChatRequest safe = new ChatDtos.ChatRequest(
                req.model(),
                req.messages(),
                req.temperature() != null ? req.temperature() : 0.2,
                req.max_tokens(),
                false
        );

        ChatDtos.ChatResponse res = client.createCompletion(safe);

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

    /** Zwraca pełny „surowy” ChatResponse jak od GPT (z zachowaniem pól). */
        public ChatDtos.ChatResponse chatRaw(ChatDtos.ChatRequest request) {
        return client.createCompletion(request);
    }
}
