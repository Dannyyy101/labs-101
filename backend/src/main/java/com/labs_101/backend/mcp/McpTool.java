package com.labs_101.backend.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tool Claude can call through the MCP server, always on the data of the
 * signed in user.
 *
 * @param inputSchema JSON schema of the arguments
 * @param annotations hints for Claude, e.g. whether the tool changes anything
 */
record McpTool(String name, String title, String description, Map<String, Object> inputSchema,
        Map<String, Object> annotations, Handler handler) {

    /** A tool that only reads. */
    static McpTool read(String name, String title, String description, Map<String, Object> inputSchema,
            Handler handler) {
        return new McpTool(name, title, description, inputSchema,
                Map.of("readOnlyHint", true, "openWorldHint", false), handler);
    }

    /** A tool that changes data, {@code destructive} when it removes something. */
    static McpTool write(String name, String title, String description, Map<String, Object> inputSchema,
            boolean destructive, Handler handler) {
        return new McpTool(name, title, description, inputSchema, Map.of("readOnlyHint", false,
                "destructiveHint", destructive, "idempotentHint", false, "openWorldHint", false), handler);
    }

    @FunctionalInterface
    interface Handler {
        /** The result is sent to Claude as JSON. */
        Object call(String userId, McpArguments arguments);
    }

    /** The tool as listed by {@code tools/list}. */
    Map<String, Object> describe() {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("title", title);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        tool.put("annotations", annotations);
        return tool;
    }

    // MARK: schema

    /** Schema of an object with the properties, {@code required} lists the names that have to be set. */
    static Map<String, Object> schema(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required.length > 0) {
            schema.put("required", List.of(required));
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    static Map<String, Object> date(String description) {
        return Map.of("type", "string", "format", "date", "description", description);
    }

    static Map<String, Object> id(String description) {
        return Map.of("type", "integer", "minimum", 1, "description", description);
    }

    static Map<String, Object> number(String description) {
        return Map.of("type", "number", "exclusiveMinimum", 0, "description", description);
    }

    static Map<String, Object> enumeration(String description, String... values) {
        return Map.of("type", "string", "enum", List.of(values), "description", description);
    }

    static Map<String, Object> integer(String description, int minimum, int maximum) {
        return Map.of("type", "integer", "minimum", minimum, "maximum", maximum, "description", description);
    }
}
