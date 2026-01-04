package com.acme.herald.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public class ChatDtos {

    @Schema(description = "Single chat message in the conversation history.")
    public record Message(
            @Schema(
                    description = "Message role (compatible with OpenAI-style chat APIs).",
                    example = "user",
                    allowableValues = {"system", "user", "assistant", "tool"}
            )
            String role,

            @Schema(
                    description = "Plain text content of the message.",
                    example = "Napisz mi krótkie podsumowanie tego dokumentu."
            )
            String content
    ) {}

    @Schema(description = "Request payload for a chat completion routed through the LLM proxy.")
    public record ChatRequest(

            @Schema(
                    description = "Model identifier from the admin LLM catalog (NOT the upstream model name).",
                    example = "openai_gpt4o"
            )
            String model,

            @Schema(
                    description = "Conversation messages (system/user/assistant). The proxy forwards them to the upstream chat API.",
                    example = """
                            [
                              {"role":"system","content":"You are a helpful assistant."},
                              {"role":"user","content":"Wypisz 3 zalety rozwiązania."}
                            ]
                            """
            )
            List<Message> messages,

            @Schema(
                    description = "Sampling temperature. If null, the proxy may apply the catalog default (or a safe fallback).",
                    example = "0.2"
            )
            Double temperature,

            @Schema(
                    description = "Max tokens limit for completion. If null, the proxy may apply the catalog default. Some upstreams require 'max_completion_tokens' instead; the proxy may transparently fallback.",
                    example = "800"
            )
            Integer max_tokens,

            @Schema(
                    description = "Streaming flag. Currently the proxy forces stream=false (non-streaming response).",
                    example = "false"
            )
            Boolean stream
    ) {}

    @Schema(description = "Single choice returned by an upstream-like chat completion API.")
    public record Choice(
            @Schema(description = "Choice index.", example = "0")
            int index,

            @Schema(description = "Assistant message for this choice.")
            Message message,

            @Schema(
                    description = "Finish reason from upstream (e.g., stop, length, content_filter).",
                    example = "stop"
            )
            String finish_reason
    ) {}

    @Schema(description = "Token usage metrics returned by the upstream-like API (if provided by the model).")
    public record Usage(
            @Schema(description = "Prompt tokens used.", example = "120")
            Integer prompt_tokens,

            @Schema(description = "Completion tokens used.", example = "240")
            Integer completion_tokens,

            @Schema(description = "Total tokens used.", example = "360")
            Integer total_tokens
    ) {}

    @Schema(description = "Raw upstream-like response from chat completion.")
    public record ChatResponse(
            @Schema(description = "Upstream response id.", example = "chatcmpl-abc123")
            String id,

            @Schema(description = "Upstream object type.", example = "chat.completion")
            String object,

            @Schema(description = "Unix timestamp (seconds).", example = "1736012345")
            long created,

            @Schema(description = "Upstream model name used for the request.", example = "gpt-4o-mini")
            String model,

            @Schema(description = "List of completion choices.")
            List<Choice> choices,

            @Schema(description = "Token usage summary (if provided by upstream).")
            Usage usage
    ) {}

    @Schema(description = "Simplified proxy reply extracted from the raw upstream response.")
    public record ProxyReply(

            @Schema(
                    description = "Extracted assistant reply content (first choice).",
                    example = "1) Szybsze wdrożenia 2) Mniej błędów 3) Lepsza kontrola jakości."
            )
            String reply,

            @Schema(
                    description = "Finish reason from upstream (first choice).",
                    example = "stop"
            )
            String finish_reason,

            @Schema(description = "Token usage summary (if available).")
            Usage usage,

            @Schema(
                    description = "Small subset of upstream metadata for debugging/UI purposes (non-stable keys).",
                    example = """
                            {"id":"chatcmpl-abc123","model":"gpt-4o-mini","created":1736012345,"choicesCount":1}
                            """
            )
            Map<String, Object> raw
    ) {}
}
