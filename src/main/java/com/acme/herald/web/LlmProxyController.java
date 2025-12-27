package com.acme.herald.web;

import com.acme.herald.domain.ChatDtos;
import com.acme.herald.service.LlmProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/llm", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LlmProxyController {
    private final LlmProxyService service;

    @PostMapping("/chat")
    public ResponseEntity<ChatDtos.ProxyReply> chat(@RequestBody ChatDtos.ChatRequest request) {
        var out = service.chat(request);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<ChatDtos.ChatResponse> chatRaw(@RequestBody ChatDtos.ChatRequest request) {
        var out = service.chatRaw(request);
        return ResponseEntity.ok(out);
    }

    // Preflight (opcjonalny)
    @RequestMapping(value = {"/chat", "/chat/completions"}, method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent().build();
    }
}
