package com.acme.herald.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder, LlmProxyHttpLogProperties props) {
        // Buffering jest MUST, jeśli chcesz logować response body i dalej parsować JSON do DTO.
        ClientHttpRequestFactory base = new SimpleClientHttpRequestFactory();
        ClientHttpRequestFactory buffering = new BufferingClientHttpRequestFactory(base);

        builder.requestFactory(buffering);

        if (props.isEnabled()) {
            builder.requestInterceptor(new LlmHttpLoggingInterceptor(props));
        }

        return builder.build();
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    static final class LlmHttpLoggingInterceptor implements ClientHttpRequestInterceptor {
        private static final Logger log = LoggerFactory.getLogger("LLM_HTTP");
        private final LlmProxyHttpLogProperties props;

        LlmHttpLoggingInterceptor(LlmProxyHttpLogProperties props) {
            this.props = props;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            String rid = UUID.randomUUID().toString().substring(0, 8);

            long t0 = System.nanoTime();
            if (!props.isOnlyErrors()) {
                logRequest(rid, request, body);
            }

            ClientHttpResponse response = execution.execute(request, body);

            // Skopiuj body (BufferingClientHttpRequestFactory już “umożliwia” ponowne czytanie,
            // ale my i tak wolimy mieć swoje bytes do logowania i pewność re-readable).
            byte[] respBytes = StreamUtils.copyToByteArray(response.getBody());
            int status = response.getStatusCode().value();
            long ms = (System.nanoTime() - t0) / 1_000_000;

            boolean shouldLog = !props.isOnlyErrors() || status >= 400;
            if (shouldLog) {
                logResponse(rid, response, respBytes, ms);
            }

            return new ReReadableClientHttpResponse(response, respBytes);
        }

        private void logRequest(String rid, HttpRequest request, byte[] body) {
            String headers = props.isLogHeaders() ? formatHeaders(request.getHeaders()) : "(headers disabled)";
            String bodyStr = props.isLogBodies() ? limit(new String(body, StandardCharsets.UTF_8), props.getMaxBodyChars()) : "(body disabled)";

            log.info("[{}] >>> {} {}\nheaders={}\nbody={}",
                    rid, request.getMethod(), request.getURI(), headers, bodyStr);
        }

        private void logResponse(String rid, ClientHttpResponse response, byte[] body, long ms) throws IOException {
            String headers = props.isLogHeaders() ? formatHeaders(response.getHeaders()) : "(headers disabled)";
            String bodyStr = props.isLogBodies() ? limit(new String(body, StandardCharsets.UTF_8), props.getMaxBodyChars()) : "(body disabled)";

            log.info("[{}] <<< {} ({}ms)\nheaders={}\nbody={}",
                    rid, response.getStatusCode().value(), ms, headers, bodyStr);
        }

        private String formatHeaders(HttpHeaders h) {
            // maskuj Authorization
            Map<String, List<String>> masked = new LinkedHashMap<>();
            h.forEach((k, v) -> {
                if ("authorization".equalsIgnoreCase(k)) {
                    masked.put(k, List.of("***"));
                } else {
                    masked.put(k, v);
                }
            });

            return masked.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
        }

        private String limit(String s, int max) {
            if (s == null) return "";
            if (max <= 0) return "";
            if (s.length() <= max) return s;
            return s.substring(0, max) + "...(truncated)";
        }
    }

    static final class ReReadableClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] body;

        ReReadableClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = (body != null ? body : new byte[0]);
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }
    }
}
