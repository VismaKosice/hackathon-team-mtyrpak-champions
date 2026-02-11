package com.pension.engine.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class PatchBuilder {

    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    private final ArrayNode ops;

    public PatchBuilder() {
        this.ops = NF.arrayNode();
    }

    public PatchBuilder(int capacity) {
        this.ops = NF.arrayNode(capacity);
    }

    public PatchBuilder add(String path, JsonNode value) {
        ObjectNode op = NF.objectNode();
        op.put("op", "add");
        op.put("path", path);
        op.set("value", value);
        ops.add(op);
        return this;
    }

    public PatchBuilder add(String path, String value) {
        return add(path, NF.textNode(value));
    }

    public PatchBuilder add(String path, double value) {
        return add(path, NF.numberNode(value));
    }

    public PatchBuilder remove(String path) {
        ObjectNode op = NF.objectNode();
        op.put("op", "remove");
        op.put("path", path);
        ops.add(op);
        return this;
    }

    public PatchBuilder replace(String path, JsonNode value) {
        ObjectNode op = NF.objectNode();
        op.put("op", "replace");
        op.put("path", path);
        op.set("value", value);
        ops.add(op);
        return this;
    }

    public PatchBuilder replace(String path, String value) {
        return replace(path, value == null ? NF.nullNode() : NF.textNode(value));
    }

    public PatchBuilder replace(String path, double value) {
        return replace(path, NF.numberNode(value));
    }

    public PatchBuilder replace(String path, Double value) {
        return replace(path, value == null ? NF.nullNode() : NF.numberNode(value));
    }

    public ArrayNode build() {
        return ops;
    }

    public static ArrayNode emptyPatch() {
        return NF.arrayNode();
    }
}
