package com.pension.engine.mutation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pension.engine.model.request.Mutation;
import com.pension.engine.model.response.CalculationMessage;
import com.pension.engine.model.state.Dossier;
import com.pension.engine.model.state.Policy;
import com.pension.engine.model.state.Situation;
import com.pension.engine.patch.PatchBuilder;
import com.pension.engine.scheme.SchemeRegistryClient;

import java.util.ArrayList;
import java.util.List;

public class AddPolicyHandler implements MutationHandler {

    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    @Override
    public MutationResult execute(Situation situation, Mutation mutation, SchemeRegistryClient schemeClient) {
        JsonNode props = mutation.getMutationProperties();
        Dossier dossier = situation.getDossier();

        // Validation
        if (dossier == null) {
            return MutationResult.critical(new CalculationMessage(
                    "CRITICAL", "DOSSIER_NOT_FOUND", "No dossier exists in the situation"));
        }

        double salary = props.path("salary").asDouble();
        if (salary < 0) {
            return MutationResult.critical(new CalculationMessage(
                    "CRITICAL", "INVALID_SALARY", "Salary must not be negative"));
        }

        double partTimeFactor = props.path("part_time_factor").asDouble();
        if (partTimeFactor < 0 || partTimeFactor > 1) {
            return MutationResult.critical(new CalculationMessage(
                    "CRITICAL", "INVALID_PART_TIME_FACTOR", "Part-time factor must be between 0 and 1"));
        }

        String schemeId = props.path("scheme_id").asText();
        String employmentStartDate = props.path("employment_start_date").asText();

        List<CalculationMessage> warnings = null;

        // Check duplicate
        List<Policy> policies = dossier.getPolicies();
        for (int i = 0; i < policies.size(); i++) {
            Policy existing = policies.get(i);
            if (existing.getSchemeId().equals(schemeId) &&
                    existing.getEmploymentStartDate().equals(employmentStartDate)) {
                if (warnings == null) warnings = new ArrayList<>(1);
                warnings.add(new CalculationMessage(
                        "WARNING", "DUPLICATE_POLICY",
                        "A policy with the same scheme_id and employment_start_date already exists"));
                break;
            }
        }

        // Application
        Policy policy = new Policy();
        String policyId = dossier.getDossierId() + "-" + dossier.nextPolicySequence();
        policy.setPolicyId(policyId);
        policy.setSchemeId(schemeId);
        policy.setEmploymentStartDate(employmentStartDate);
        policy.setSalary(salary);
        policy.setPartTimeFactor(partTimeFactor);
        policy.setAttainablePension(null);
        policy.setProjections(null);

        int newIndex = dossier.getPolicies().size(); // index before add
        dossier.getPolicies().add(policy);

        // Build forward patch value manually (avoids mapper.valueToTree overhead)
        ObjectNode policyNode = NF.objectNode();
        policyNode.put("policy_id", policyId);
        policyNode.put("scheme_id", schemeId);
        policyNode.put("employment_start_date", employmentStartDate);
        policyNode.put("salary", salary);
        policyNode.put("part_time_factor", partTimeFactor);
        policyNode.putNull("attainable_pension");
        policyNode.putNull("projections");

        ArrayNode fwd = new PatchBuilder(1).add("/dossier/policies/-", policyNode).build();
        ArrayNode bwd = new PatchBuilder(1).remove("/dossier/policies/" + newIndex).build();

        if (warnings != null) {
            return MutationResult.warningsWithPatches(warnings, fwd, bwd);
        }
        return MutationResult.successWithPatches(fwd, bwd);
    }
}
