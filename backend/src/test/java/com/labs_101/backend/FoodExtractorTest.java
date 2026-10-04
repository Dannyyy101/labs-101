package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.foodExtractor.FoodExtractor;
import com.labs_101.backend.foodExtractor.FoodExtractor.FoodPrompt;

@SpringBootTest
public class FoodExtractorTest {

    @Autowired
    private FoodExtractor extractor;

    @Test
    void TestSplitElements() {
        List<FoodPrompt> extracted = extractor.splitElements("eine Banane ein Apfel ein Esslöffel Zimt");

        assertEquals(3, extracted.size());

        FoodPrompt first = extracted.get(0);

        assertEquals(1.0, first.getAmount());
        assertEquals("g", first.getPortion());
        assertEquals("Banane", first.getName());

        FoodPrompt third = extracted.get(2);
        assertEquals(1.0, third.getAmount());
        assertEquals("Esslöffel", third.getPortion());
        assertEquals("Zimt", third.getName());
    }
}
