package com.pension.engine.mutation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.pension.engine.model.request.Mutation;
import com.pension.engine.model.response.CalculationMessage;
import com.pension.engine.model.state.Dossier;
import com.pension.engine.model.state.Person;
import com.pension.engine.model.state.Situation;
import com.pension.engine.patch.PatchBuilder;
import com.pension.engine.scheme.SchemeRegistryClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class CreateDossierHandler implements MutationHandler {

    private final ObjectMapper mapper;

    public CreateDossierHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MutationResult execute(Situation situation, Mutation mutation, SchemeRegistryClient schemeClient) {
        JsonNode props = mutation.getMutationProperties();

        // Validation
        if (situation.getDossier() != null) {
            return MutationResult.critical(new CalculationMessage(
                    "CRITICAL", "DOSSIER_ALREADY_EXISTS", "A dossier already exists in the situation"));
        }

        String name = props.path("name").asText("");
        if (name.isBlank()) {
            return MutationResult.critical(new CalculationMessage(
                    "CRITICAL", "INVALID_NAME", "Name is empty or blank"));
        }

        String birthDateStr = props.path("birth_date").asText("");
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            if (birthDate.isAfter(LocalDate.now())) {
                return MutationResult.critical(new CalculationMessage(
                        "CRITICAL", "INVALID_BIRTH_DATE", "Birth date is in the future"));
            }
        } catch (DateTimeParseException e) {
            return MutationResult.critical(new CalculationMessage(
                    "CRITICAL", "INVALID_BIRTH_DATE", "Birth date is not a valid date"));
        }

        // Application
        String dossierId = props.path("dossier_id").asText();
        String personId = props.path("person_id").asText();

        Dossier dossier = new Dossier();
        dossier.setDossierId(dossierId);
        dossier.setStatus("ACTIVE");
        dossier.setRetirementDate(null);

        Person person = new Person(personId, "PARTICIPANT", name, birthDateStr);
        dossier.getPersons().add(person);

        situation.setDossier(dossier);

        // Build patches: forward = add /dossier {serialized}, backward = remove /dossier
        JsonNode dossierNode = mapper.valueToTree(dossier);
        ArrayNode fwd = new PatchBuilder(1).add("/dossier", dossierNode).build();
        ArrayNode bwd = new PatchBuilder(1).remove("/dossier").build();

        return MutationResult.successWithPatches(fwd, bwd);
    }
}
