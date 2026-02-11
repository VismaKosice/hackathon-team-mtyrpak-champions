package com.pension.engine.scheme;

import com.pension.engine.model.state.Policy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SchemeRegistryClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final ConcurrentHashMap<String, Double> cache = new ConcurrentHashMap<>();

    public SchemeRegistryClient(
            WebClient schemeRegistryWebClient,
            @Value("${scheme.registry.url:}") String baseUrl) {
        this.webClient = schemeRegistryWebClient;
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isEmpty();
    }

    public Map<String, Double> getAccrualRates(List<Policy> policies) {
        if (!isEnabled()) {
            return null;
        }

        // Collect unique scheme IDs
        Set<String> uniqueSchemeIds = new HashSet<>();
        for (Policy policy : policies) {
            uniqueSchemeIds.add(policy.getSchemeId());
        }

        // Check cache for all, collect missing
        Map<String, Double> result = new HashMap<>(uniqueSchemeIds.size());
        Set<String> toFetch = new HashSet<>();
        for (String schemeId : uniqueSchemeIds) {
            Double cached = cache.get(schemeId);
            if (cached != null) {
                result.put(schemeId, cached);
            } else {
                toFetch.add(schemeId);
            }
        }

        if (toFetch.isEmpty()) {
            return result;
        }

        // Fetch missing in parallel using reactive WebClient
        // .block() is safe here because we run on CALC_SCHEDULER / gRPC thread pool, never event loop
        Map<String, Double> fetched = Flux.fromIterable(toFetch)
                .flatMap(schemeId -> fetchAccrualRate(schemeId)
                        .map(rate -> Map.entry(schemeId, rate)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block(Duration.ofSeconds(3));

        if (fetched != null) {
            for (Map.Entry<String, Double> entry : fetched.entrySet()) {
                cache.put(entry.getKey(), entry.getValue());
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private Mono<Double> fetchAccrualRate(String schemeId) {
        return webClient.get()
                .uri("/schemes/" + schemeId)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(2))
                .map(body -> {
                    Object accrualRate = body.get("accrual_rate");
                    if (accrualRate instanceof Number) {
                        return ((Number) accrualRate).doubleValue();
                    }
                    return 0.02;
                })
                .onErrorReturn(0.02);
    }
}
