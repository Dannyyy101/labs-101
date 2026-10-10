package com.labs_101.backend.foodImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.labs_101.backend.foodImage.GeminiClient.DetectedFood;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Against a local server, the models answer like Gemini does when it is overloaded. */
public class GeminiClientFallbackTest {
    private static final String ANSWER = """
            {"candidates":[{"content":{"parts":[{"text":"[{\\"name\\":\\"Banane\\",\\"amount\\":1,\\"unit\\":\\"Stück\\",\\"gramsPerUnit\\":120}]","thoughtSignature":"abc"}]},"finishReason":"STOP"}]}
            """;

    private HttpServer server;
    private final List<String> asked = new CopyOnWriteArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/models/", (exchange) -> {
            String model = exchange.getRequestURI().getPath().replaceAll(".*/models/([^:]+):.*", "$1");
            asked.add(model);
            switch (model) {
                case "slow" -> sleep(2000);
                case "busy" -> respond(exchange, 503, "{\"error\":{\"message\":\"high demand\"}}");
                case "denied" -> respond(exchange, 403, "{\"error\":{\"message\":\"API key not valid\"}}");
                default -> respond(exchange, 200, ANSWER);
            }
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private GeminiClient client(String models) {
        return new GeminiClient("key", models, "http://localhost:" + server.getAddress().getPort(), 300);
    }

    @Test
    void testTimeoutAndOverloadTryTheNextModel() {
        List<DetectedFood> foods = client("slow,busy,ok").detect(new byte[] { 1 }, "image/jpeg", null);

        assertEquals(List.of(new DetectedFood("Banane", 1.0, "Stück", 120.0)), foods);
        assertEquals(List.of("slow", "busy", "ok"), asked);
    }

    @Test
    void testAllModelsFailing() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> client("slow,busy").detect(new byte[] { 1 }, "image/jpeg", null));

        assertTrue(e.getMessage().contains("slow: no answer in time"), e.getMessage());
        assertTrue(e.getMessage().contains("busy: 503 high demand"), e.getMessage());
    }

    @Test
    void testWrongKeyStopsRightAway() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> client("denied,ok").detect(new byte[] { 1 }, "image/jpeg", null));

        assertTrue(e.getMessage().contains("API key not valid"), e.getMessage());
        assertEquals(List.of("denied"), asked);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
