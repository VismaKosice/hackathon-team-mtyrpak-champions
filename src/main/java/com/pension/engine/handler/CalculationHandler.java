package com.pension.engine.handler;

import com.pension.engine.engine.CalculationEngine;
import com.pension.engine.model.request.CalculationRequest;
import com.pension.engine.model.response.ErrorResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CalculationHandler {

    private final CalculationEngine engine;

    public CalculationHandler(CalculationEngine engine) {
        this.engine = engine;
    }

    public Mono<ServerResponse> handleCalculation(ServerRequest request) {
        return request.bodyToMono(CalculationRequest.class)
                .flatMap(calcRequest -> {
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

                    return engine.process(calcRequest)
                            .flatMap(response -> ServerResponse.ok().bodyValue(response));
                })
                .onErrorResume(e -> ServerResponse.status(500)
                        .bodyValue(new ErrorResponse(500, "Internal server error: " + e.getMessage())));
    }
}
