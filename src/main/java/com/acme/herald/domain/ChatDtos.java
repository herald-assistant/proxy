package com.acme.herald.domain;

import java.util.List;
import java.util.Map;

public class ChatDtos {

    public record Message(String role, String content) {}

    public record ChatRequest(
            String model,
            List<Message> messages,
            Double temperature,
            Integer max_tokens,
            Boolean stream
    ) {}

    public record Choice(int index, Message message, String finish_reason) {}

    public record Usage(Integer prompt_tokens, Integer completion_tokens, Integer total_tokens) {}

    public record ChatResponse(
            String id,
            String object,
            long created,
            String model,
            List<Choice> choices,
            Usage usage
    ) {}

    public record ProxyReply(String reply, String finish_reason, Usage usage, Map<String, Object> raw) {}
}
