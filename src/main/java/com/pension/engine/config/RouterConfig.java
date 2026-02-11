package com.pension.engine.config;

import com.pension.engine.handler.CalculationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration(proxyBeanMethods = false)
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(CalculationHandler handler) {
        return RouterFunctions.route(POST("/calculation-requests"), handler::handleCalculation);
    }
}
