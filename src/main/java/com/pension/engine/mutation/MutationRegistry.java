package com.pension.engine.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class MutationRegistry {

    private final Map<String, MutationHandler> handlers;

    public MutationRegistry(ObjectMapper mapper) {
        handlers = new HashMap<>(8);
        handlers.put("create_dossier", new CreateDossierHandler(mapper));
        handlers.put("add_policy", new AddPolicyHandler(mapper));
        handlers.put("apply_indexation", new ApplyIndexationHandler());
        handlers.put("calculate_retirement_benefit", new CalculateRetirementBenefitHandler());
        handlers.put("project_future_benefits", new ProjectFutureBenefitsHandler());
    }

    public MutationHandler getHandler(String mutationDefinitionName) {
        return handlers.get(mutationDefinitionName);
    }
}
