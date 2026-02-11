package com.pension.engine.mutation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.pension.engine.model.response.CalculationMessage;

import java.util.List;

public class MutationResult {

    public static final MutationResult SUCCESS = new MutationResult(List.of(), false, null, null);

    private final List<CalculationMessage> messages;
    private final boolean critical;
    private final ArrayNode forwardPatch;
    private final ArrayNode backwardPatch;

    private MutationResult(List<CalculationMessage> messages, boolean critical,
                           ArrayNode forwardPatch, ArrayNode backwardPatch) {
        this.messages = messages;
        this.critical = critical;
        this.forwardPatch = forwardPatch;
        this.backwardPatch = backwardPatch;
    }

    public static MutationResult success() {
        return SUCCESS;
    }

    public static MutationResult successWithPatches(ArrayNode forwardPatch, ArrayNode backwardPatch) {
        return new MutationResult(List.of(), false, forwardPatch, backwardPatch);
    }

    public static MutationResult withMessages(List<CalculationMessage> messages) {
        boolean hasCritical = false;
        for (CalculationMessage msg : messages) {
            if ("CRITICAL".equals(msg.getLevel())) {
                hasCritical = true;
                break;
            }
        }
        return new MutationResult(messages, hasCritical, null, null);
    }

    public static MutationResult critical(CalculationMessage message) {
        return new MutationResult(List.of(message), true, null, null);
    }

    public static MutationResult critical(List<CalculationMessage> messages) {
        return new MutationResult(messages, true, null, null);
    }

    public static MutationResult warning(CalculationMessage message) {
        return new MutationResult(List.of(message), false, null, null);
    }

    public static MutationResult warnings(List<CalculationMessage> messages) {
        return new MutationResult(messages, false, null, null);
    }

    public static MutationResult warningsWithPatches(List<CalculationMessage> messages,
                                                     ArrayNode forwardPatch, ArrayNode backwardPatch) {
        return new MutationResult(messages, false, forwardPatch, backwardPatch);
    }

    public List<CalculationMessage> getMessages() { return messages; }
    public boolean isCritical() { return critical; }
    public ArrayNode getForwardPatch() { return forwardPatch; }
    public ArrayNode getBackwardPatch() { return backwardPatch; }
    public boolean hasPatches() { return forwardPatch != null; }
}
