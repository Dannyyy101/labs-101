package com.labs_101.backend.foodImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks Gemini which foods are on a photo. Only the names, amounts and grams
 * come from the model, matching them against our foods is done by
 * {@link com.labs_101.backend.foodExtractor.FoodExtractor} like for text.
 * Without {@code gemini.api-key} photos can't be uploaded.
 * <p>
 * {@code gemini.model} is a comma separated list: when a model is overloaded
 * (503), out of quota (429), gone (404) or doesn't answer in time, the next one
 * is asked. Flash models of the free tier are often overloaded for a while.
 */
@Component
public class GeminiClient {

    private static final JsonMapper JSON = new JsonMapper();
    static final int READ_TIMEOUT_MS = 60_000;

    private final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    static final String PROMPT = """
            Du analysierst ein Foto einer Mahlzeit für eine Kalorien-Tracking-App.
            Liste jedes sichtbare Lebensmittel und Getränk einzeln auf, zusammengesetzte Gerichte nur dann als ein
            Eintrag, wenn die Bestandteile nicht erkennbar sind (z. B. "Lasagne").
            - name: kurzer deutscher Name wie im Bundeslebensmittelschlüssel, mit Zubereitung falls erkennbar,
              z. B. "Reis gekocht", "Hähnchenbrust gebraten", "Banane", "Vollkornbrot", "Butter".
            - unit: "g" oder "ml" für lose Mengen (Reis, Soße, Getränke), sonst eine Portion wie "Stück", "Scheibe",
              "Esslöffel", "Tasse", "Glas".
            - amount: Anzahl der Einheiten, bei g/ml die geschätzte Menge.
            - gramsPerUnit: geschätzte Gramm einer Einheit, bei g und ml immer 1.
            Schätze Mengen anhand von Teller, Besteck und Verpackungen. Ist kein Essen zu sehen, gib eine leere Liste zurück.
            """;

    // the user knows what they eat, the photo still decides the amounts
    static final String DESCRIPTION_PROMPT = """
            Der Nutzer beschreibt, was auf dem Foto zu sehen ist. Nutze das, um die Lebensmittel und Zutaten richtig
            zu benennen, auch wenn sie auf dem Foto nicht eindeutig zu erkennen sind. Nennt der Nutzer Mengen, nimm
            diese, sonst schätze sie anhand des Fotos.
            Beschreibung des Nutzers: %s
            """;

    // OpenAPI subset Gemini understands for responseSchema
    static final Map<String, Object> SCHEMA = Map.of(
            "type", "ARRAY",
            "items", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "name", Map.of("type", "STRING"),
                            "amount", Map.of("type", "NUMBER"),
                            "unit", Map.of("type", "STRING"),
                            "gramsPerUnit", Map.of("type", "NUMBER")),
                    "required", List.of("name", "amount", "unit", "gramsPerUnit"),
                    "propertyOrdering", List.of("name", "amount", "unit", "gramsPerUnit")));

    public record DetectedFood(String name, Double amount, String unit, Double gramsPerUnit) {
    }

    record Response(List<Candidate> candidates) {
    }

    record Candidate(Content content, String finishReason) {
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }

    private final RestClient restClient;
    private final String apiKey;
    private final List<String> models;

    // the one Spring uses, the other one is for tests with a short timeout
    @Autowired
    GeminiClient(@Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash,gemini-3.6-flash,gemini-3.5-flash-lite}") String models,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this(apiKey, models, baseUrl, READ_TIMEOUT_MS);
    }

    GeminiClient(String apiKey, String models, String baseUrl, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.models = Arrays.stream(models.split(",")).map(String::strip).filter((m) -> !m.isEmpty()).toList();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Throws on HTTP errors (e.g. 429 when the free quota is used up) and unreadable answers.
     *
     * @param description optional hint of the user what is on the photo
     */
    public List<DetectedFood> detect(byte[] image, String contentType, String description) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("inlineData", Map.of("mimeType", contentType,
                                "data", Base64.getEncoder().encodeToString(image))),
                        Map.of("text", prompt(description))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", SCHEMA,
                        "temperature", 0.2));

        List<String> failures = new ArrayList<>();
        for (String model : models) {
            try {
                Response response = restClient.post()
                        .uri("/models/{model}:generateContent", model)
                        .header("x-goog-api-key", apiKey)
                        .body(body)
                        .retrieve()
                        .body(Response.class);
                return parse(response);
            } catch (HttpStatusCodeException e) {
                // e.g. 400 (bad request) or 403 (wrong key) would fail the same way with every model
                if (!tryNextModel(e.getStatusCode().value()))
                    throw new IllegalStateException(model + ": " + e.getStatusCode() + " " + errorMessage(e), e);
                failures.add(model + ": " + e.getStatusCode().value() + " " + errorMessage(e));
            } catch (RestClientException e) {
                // connection problems and timeouts, a read timeout is reported as plain RestClientException
                // "Error while extracting response ... application/octet-stream" (no headers arrived)
                failures.add(model + ": " + (isTimeout(e) ? "no answer in time" : e.getMessage()));
            }
            logger.info("Gemini model failed, trying the next one: {}", failures.getLast());
        }
        throw new IllegalStateException("All Gemini models failed: " + String.join("; ", failures));
    }

    static boolean tryNextModel(int status) {
        return status == HttpStatus.NOT_FOUND.value() || status == HttpStatus.TOO_MANY_REQUESTS.value()
                || status >= 500;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause())
            if (cause instanceof java.net.SocketTimeoutException)
                return true;
        return false;
    }

    // the "message" of Gemini's error body, e.g. "This model is currently experiencing high demand"
    private static String errorMessage(HttpStatusCodeException e) {
        try {
            return JSON.readTree(e.getResponseBodyAsString()).path("error").path("message").asString(e.getStatusText());
        } catch (RuntimeException parseError) {
            return e.getStatusText();
        }
    }

    static String prompt(String description) {
        return description == null || description.isBlank() ? PROMPT
                : PROMPT + "\n" + DESCRIPTION_PROMPT.formatted(description.strip());
    }

    static List<DetectedFood> parse(Response response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty())
            throw new IllegalStateException("Gemini returned no candidates");
        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null)
            throw new IllegalStateException("Gemini returned no content, finish reason " + candidate.finishReason());

        String text = String.join("", candidate.content().parts().stream()
                .map((part) -> part.text() == null ? "" : part.text()).toList());
        List<DetectedFood> foods = JSON.readValue(text, new TypeReference<List<DetectedFood>>() {
        });
        return foods.stream()
                .filter((food) -> food.name() != null && !food.name().isBlank())
                .toList();
    }
}
