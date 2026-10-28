package com.labs_101.backend.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.exception.BaseException;
import com.labs_101.backend.security.CurrentUser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * MCP server (Streamable HTTP, without sessions or streams) so Claude can read
 * the data of the signed in user, see {@link McpTools} for what it can read.
 * Claude signs in with Zitadel like the apps, the token comes in the
 * {@code Authorization} header of every request.
 */
@RestController
@RequestMapping(McpController.PATH)
public class McpController {
    public static final String PATH = "/api/mcp";

    /** Newest first, the first one is offered to clients asking for a version we don't know. */
    static final List<String> PROTOCOL_VERSIONS = List.of("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05");

    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private static final String INSTRUCTIONS = """
            Data of the user's labs-101 account: strength workouts, runs, Apple Health data, nutrition and the \
            calendar. The tools only read, except track_food and delete_tracked_food, which change the nutrition \
            log: search the food with search_foods first. Dates are days (YYYY-MM-DD) in Europe/Berlin, weights \
            are in kg, distances in meters, times in seconds and energy in kcal.""";

    private final McpTools tools;
    private final JsonMapper json;
    private final MessageSource messageSource;

    McpController(McpTools tools, JsonMapper json, MessageSource messageSource) {
        this.tools = tools;
        this.json = json;
        this.messageSource = messageSource;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handle(@CurrentUser String userId, @RequestBody JsonNode message) {
        if (!message.isObject()) {
            // batches were removed from the protocol in 2025-06-18
            return ResponseEntity.badRequest().body(error(null, -32600, "expected a single JSON-RPC message"));
        }
        JsonNode id = message.get("id");
        String method = message.path("method").asString(null);
        // notifications and answers to requests of ours (we never send any) need no answer
        if (method == null || id == null || id.isNull()) {
            return ResponseEntity.accepted().build();
        }

        JsonNode params = message.path("params");
        return ResponseEntity.ok(switch (method) {
            case "initialize" -> result(id, initialize(params));
            case "ping" -> result(id, Map.of());
            case "tools/list" -> result(id, Map.of("tools", tools.all().stream().map(McpTool::describe).toList()));
            case "tools/call" -> call(id, userId, params);
            default -> error(id, -32601, "unknown method " + method);
        });
    }

    /** There are no streams from the server to the client. */
    @GetMapping
    public ResponseEntity<Void> stream() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    /** There are no sessions that could be ended. */
    @DeleteMapping
    public ResponseEntity<Void> endSession() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    private Map<String, Object> initialize(JsonNode params) {
        String requested = params.path("protocolVersion").asString(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSIONS.contains(requested) ? requested : PROTOCOL_VERSIONS.getFirst());
        result.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        result.put("serverInfo", Map.of("name", "labs-101", "title", "labs-101", "version", "1.0.0"));
        result.put("instructions", INSTRUCTIONS);
        return result;
    }

    private Map<String, Object> call(JsonNode id, String userId, JsonNode params) {
        String name = params.path("name").asString(null);
        McpTool tool = name == null ? null : tools.find(name).orElse(null);
        if (tool == null) {
            return error(id, -32602, "unknown tool " + name);
        }
        try {
            Object value = tool.handler().call(userId, new McpArguments(params.get("arguments")));
            return result(id, Map.of("content", List.of(text(json.writeValueAsString(value))), "isError", false));
        } catch (IllegalArgumentException e) {
            return result(id, toolError(e.getMessage()));
        } catch (BaseException e) {
            return result(id, toolError(messageSource.getMessage(e.getCode(), e.getArgs(), e.getCode(), Locale.GERMAN)));
        } catch (RuntimeException e) {
            log.error("MCP tool {} failed", name, e);
            return result(id, toolError("the tool failed, please try again later"));
        }
    }

    // MARK: JSON-RPC

    private static Map<String, Object> toolError(String message) {
        return Map.of("content", List.of(text(message)), "isError", true);
    }

    private static Map<String, Object> text(String text) {
        return Map.of("type", "text", "text", text);
    }

    private static Map<String, Object> result(JsonNode id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> error(JsonNode id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }
}
