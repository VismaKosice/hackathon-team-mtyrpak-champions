package com.pension.engine.scheme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pension.engine.model.state.Policy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class SchemeRegistryClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String baseUrl;
    private final boolean enabled;
    private final ConcurrentHashMap<String, Double> cache = new ConcurrentHashMap<>();

    public SchemeRegistryClient(@Value("${scheme.registry.url:}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.enabled = baseUrl != null && !baseUrl.isEmpty();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Double> getAccrualRates(List<Policy> policies) {
        if (!enabled) {
            return null;
        }

        // Collect unique scheme IDs
        Set<String> uniqueSchemeIds = new HashSet<>();
        for (Policy policy : policies) {
            uniqueSchemeIds.add(policy.getSchemeId());
        }

        // Check cache for all, collect missing
        Map<String, Double> result = new HashMap<>(uniqueSchemeIds.size());
        List<String> toFetch = new ArrayList<>();
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

        // Fetch missing in parallel using CompletableFuture
        CompletableFuture<Map.Entry<String, Double>>[] futures = new CompletableFuture[toFetch.size()];
        for (int i = 0; i < toFetch.size(); i++) {
            String schemeId = toFetch.get(i);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/schemes/" + schemeId))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            futures[i] = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            JsonNode node = MAPPER.readTree(response.body());
                            double accrualRate = node.path("accrual_rate").asDouble(0.02);
                            return Map.entry(schemeId, accrualRate);
                        } catch (Exception e) {
                            return Map.entry(schemeId, 0.02);
                        }
                    })
                    .exceptionally(e -> Map.entry(schemeId, 0.02));
        }

        try {
            CompletableFuture.allOf(futures).get(3, TimeUnit.SECONDS);
            for (CompletableFuture<Map.Entry<String, Double>> f : futures) {
                Map.Entry<String, Double> entry = f.get();
                cache.put(entry.getKey(), entry.getValue());
                result.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            // Timeout or error - default all unfetched to 0.02
            for (String schemeId : toFetch) {
                result.putIfAbsent(schemeId, 0.02);
            }
        }

        return result;
    }
}
