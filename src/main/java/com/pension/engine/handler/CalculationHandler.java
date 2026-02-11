package com.pension.engine.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.pension.engine.engine.CalculationEngine;
import com.pension.engine.model.request.CalculationRequest;
import com.pension.engine.model.response.CalculationResponse;
import com.pension.engine.model.response.ErrorResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CalculationHandler {

    private final CalculationEngine engine;
    private final ObjectReader requestReader;
    private final ObjectWriter responseWriter;

    public CalculationHandler(CalculationEngine engine, ObjectMapper mapper) {
        this.engine = engine;
        this.requestReader = mapper.readerFor(CalculationRequest.class);
        this.responseWriter = mapper.writerFor(CalculationResponse.class);
    }

    public Mono<ServerResponse> handleCalculation(ServerRequest request) {
        return request.bodyToMono(byte[].class)
                .flatMap(bytes -> {
                    try {
                        CalculationRequest calcRequest = requestReader.readValue(bytes);

                        // Validation
                        if (calcRequest.getTenantId() == null || calcRequest.getTenantId().isEmpty()) {
                            return ServerResponse.badRequest()
                                    .bodyValue(new ErrorResponse(400, "tenant_id is required"));
                        }
                        if (calcRequest.getCalculationInstructions() == null ||
                                calcRequest.getCalculationInstructions().getMutations() == null ||
                                calcRequest.getCalculationInstructions().getMutations().isEmpty()) {
                            return ServerResponse.badRequest()
                                    .bodyValue(new ErrorResponse(400, "At least one mutation is required"));
                        }

                        CalculationResponse response = engine.processSync(calcRequest);
                        byte[] responseBytes = responseWriter.writeValueAsBytes(response);
                        return ServerResponse.ok()
                                .header("Content-Type", "application/json")
                                .bodyValue(responseBytes);
                    } catch (Exception e) {
                        return ServerResponse.status(500)
                                .bodyValue(new ErrorResponse(500, e.getMessage()));
                    }
                })
                .onErrorResume(e -> ServerResponse.status(500)
                        .bodyValue(new ErrorResponse(500, "Internal server error: " + e.getMessage())));
    }
}
