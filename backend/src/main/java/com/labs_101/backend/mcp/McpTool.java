package com.labs_101.backend.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tool Claude can call through the MCP server, it only reads the data of the
 * signed in user.
 *
 * @param inputSchema JSON schema of the arguments
 */
record McpTool(String name, String title, String description, Map<String, Object> inputSchema, Handler handler) {

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
        tool.put("annotations", Map.of("readOnlyHint", true, "openWorldHint", false));
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

    static Map<String, Object> integer(String description, int minimum, int maximum) {
        return Map.of("type", "integer", "minimum", minimum, "maximum", maximum, "description", description);
    }
}
