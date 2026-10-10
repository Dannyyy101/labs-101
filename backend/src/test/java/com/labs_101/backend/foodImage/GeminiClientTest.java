package com.labs_101.backend.foodImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.labs_101.backend.foodImage.GeminiClient.Candidate;
import com.labs_101.backend.foodImage.GeminiClient.Content;
import com.labs_101.backend.foodImage.GeminiClient.DetectedFood;
import com.labs_101.backend.foodImage.GeminiClient.Part;
import com.labs_101.backend.foodImage.GeminiClient.Response;

public class GeminiClientTest {

    private static Response answer(String... parts) {
        return new Response(List.of(new Candidate(
                new Content(List.of(parts).stream().map(Part::new).toList()), "STOP")));
    }

    @Test
    void testParseJoinsPartsAndDropsNamelessFoods() {
        List<DetectedFood> foods = GeminiClient.parse(answer(
                "[{\"name\":\"Banane\",\"amount\":1,\"unit\":\"Stück\",\"gramsPerUnit\":120},",
                "{\"name\":\" \",\"amount\":1,\"unit\":\"g\",\"gramsPerUnit\":1}]"));

        assertEquals(List.of(new DetectedFood("Banane", 1.0, "Stück", 120.0)), foods);
    }

    @Test
    void testParseEmptyList() {
        assertEquals(List.of(), GeminiClient.parse(answer("[]")));
    }

    @Test
    void testParseBlockedAnswer() {
        assertThrows(IllegalStateException.class,
                () -> GeminiClient.parse(new Response(List.of(new Candidate(null, "SAFETY")))));
        assertThrows(IllegalStateException.class, () -> GeminiClient.parse(new Response(List.of())));
    }

    @Test
    void testPromptWithDescription() {
        assertEquals(GeminiClient.PROMPT, GeminiClient.prompt("  "));
        String prompt = GeminiClient.prompt(" Linsensuppe mit Würstchen ");
        assertTrue(prompt.startsWith(GeminiClient.PROMPT));
        assertTrue(prompt.contains("Beschreibung des Nutzers: Linsensuppe mit Würstchen\n"));
    }

    @Test
    void testOnlyModelSpecificErrorsTryTheNextModel() {
        // overloaded, out of quota, gone
        assertTrue(GeminiClient.tryNextModel(503));
        assertTrue(GeminiClient.tryNextModel(500));
        assertTrue(GeminiClient.tryNextModel(429));
        assertTrue(GeminiClient.tryNextModel(404));
        // the request or the key is wrong, every model would say the same
        assertFalse(GeminiClient.tryNextModel(400));
        assertFalse(GeminiClient.tryNextModel(403));
    }
}
