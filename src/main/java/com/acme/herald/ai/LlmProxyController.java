package com.acme.herald.ai;

import com.acme.herald.domain.ChatDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/llm", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(
        name = "LlmProxyController",
        description = "LLM proxy endpoints. Routes chat requests to an upstream model configured in the admin LLM catalog."
)
public class LlmProxyController {
    private final LlmProxyService service;

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Chat (simplified reply)",
            description = "Executes a chat completion using a model identified by request.model (modelId from the admin catalog) and returns a simplified reply (text + finish_reason + usage + small raw metadata)."
    )
    public ResponseEntity<ChatDtos.ProxyReply> chat(@RequestBody ChatDtos.ChatRequest request) {
        var out = service.chat(request);
        return ResponseEntity.ok(out);
    }

    @PostMapping(value = "/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Chat (raw upstream response)",
            description = "Executes a chat completion using a model identified by request.model (modelId from the admin catalog) and returns the full upstream-like response structure."
    )
    public ResponseEntity<ChatDtos.ChatResponse> chatRaw(@RequestBody ChatDtos.ChatRequest request) {
        var out = service.chatRaw(request);
        return ResponseEntity.ok(out);
    }

    @RequestMapping(value = {"/chat", "/chat/completions"}, method = RequestMethod.OPTIONS)
    @Operation(
            summary = "CORS preflight (optional)",
            description = "Optional preflight endpoint for browsers."
    )
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent().build();
    }
}
