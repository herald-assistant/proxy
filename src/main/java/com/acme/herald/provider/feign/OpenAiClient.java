package com.acme.herald.provider.feign;

import com.acme.herald.config.OpenAiFeignConfig;
import com.acme.herald.domain.ChatDtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "openAiClient",
        url = "${llm.gpt.baseUrl}",
        configuration = OpenAiFeignConfig.class
)
public interface OpenAiClient {

    @PostMapping // path pusty, bo baseUrl może już wskazywać na /chat/completions
    ChatDtos.ChatResponse createCompletion(ChatDtos.ChatRequest request);
}
