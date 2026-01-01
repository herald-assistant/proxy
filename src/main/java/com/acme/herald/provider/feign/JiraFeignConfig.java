package com.acme.herald.provider.feign;


import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLContext;

public class JiraFeignConfig {

    @Bean
    CloseableHttpClient jiraCloseableHttpClient() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();

        var tlsStrategy = new DefaultClientTlsStrategy(
                sslContext,
                HostnameVerificationPolicy.CLIENT,
                NoopHostnameVerifier.INSTANCE
        );

        var cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .build();

        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    @Bean
    Client feignClient(CloseableHttpClient jiraCloseableHttpClient) {
        return new ApacheHttp5Client(jiraCloseableHttpClient);
    }
}