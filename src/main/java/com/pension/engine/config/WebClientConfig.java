package com.pension.engine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Bean
    public WebClient schemeRegistryWebClient(@Value("${scheme.registry.url:}") String baseUrl) {
        ConnectionProvider provider = ConnectionProvider.builder("scheme")
                .maxConnections(128)
                .maxIdleTime(Duration.ofSeconds(30))
                .pendingAcquireTimeout(Duration.ofSeconds(3))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .keepAlive(true)
                .responseTimeout(Duration.ofSeconds(2));

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }
}
