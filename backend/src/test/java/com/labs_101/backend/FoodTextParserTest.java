package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.labs_101.backend.foodExtractor.FoodTextParser;
import com.labs_101.backend.foodExtractor.FoodTextParser.ParsedFood;

public class FoodTextParserTest {

    private static void assertParsed(ParsedFood actual, Double amount, String unit, String name) {
        assertEquals(amount, actual.amount(), "amount of " + actual);
        assertEquals(unit, actual.unit(), "unit of " + actual);
        assertEquals(name, actual.name(), "name of " + actual);
    }

    @Test
    void testSplitsBySeparators() {
        List<ParsedFood> parsed = FoodTextParser.parse("2 Eier, eine Tasse Cappuccino, 1 Banane und 30 g Mandeln");

        assertEquals(4, parsed.size());
        assertParsed(parsed.get(0), 2.0, null, "Eier");
        assertParsed(parsed.get(1), 1.0, "Tasse", "Cappuccino");
        assertParsed(parsed.get(2), 1.0, null, "Banane");
        assertParsed(parsed.get(3), 30.0, "g", "Mandeln");
    }

    @Test
    void testSplitsByAmountsWithoutSeparators() {
        List<ParsedFood> parsed = FoodTextParser.parse("eine Banane ein Apfel ein Esslöffel Zimt");

        assertEquals(3, parsed.size());
        assertParsed(parsed.get(0), 1.0, null, "Banane");
        assertParsed(parsed.get(1), 1.0, null, "Apfel");
        assertParsed(parsed.get(2), 1.0, "Esslöffel", "Zimt");
    }

    @Test
    void testOtherSeparators() {
        List<ParsedFood> parsed = FoodTextParser.parse("Reis; Brokkoli + 2 Eier & Toast\nSkyr sowie Honig");

        assertEquals(List.of("Reis", "Brokkoli", "Eier", "Toast", "Skyr", "Honig"),
                parsed.stream().map(ParsedFood::name).toList());
    }

    @Test
    void testMultiWordNamesWithoutAmount() {
        List<ParsedFood> parsed = FoodTextParser.parse("griechischer Joghurt");

        assertEquals(1, parsed.size());
        assertParsed(parsed.getFirst(), null, null, "griechischer Joghurt");
    }

    @Test
    void testUnitAttachedToNumber() {
        assertParsed(FoodTextParser.parse("30g Mandeln").getFirst(), 30.0, "g", "Mandeln");
        assertParsed(FoodTextParser.parse("250ml Milch").getFirst(), 250.0, "ml", "Milch");
    }

    @Test
    void testDecimalCommaIsNoSeparator() {
        List<ParsedFood> parsed = FoodTextParser.parse("1,5 kg Kartoffeln, 0.5 l Milch");

        assertEquals(2, parsed.size());
        assertParsed(parsed.get(0), 1500.0, "g", "Kartoffeln");
        assertParsed(parsed.get(1), 500.0, "ml", "Milch");
    }

    @Test
    void testUnitSpellingsAreNormalized() {
        assertParsed(FoodTextParser.parse("3 EL Haferflocken").getFirst(), 3.0, "Esslöffel", "Haferflocken");
        assertParsed(FoodTextParser.parse("2 Tassen Kaffee").getFirst(), 2.0, "Tasse", "Kaffee");
        assertParsed(FoodTextParser.parse("2 Scheiben Brot").getFirst(), 2.0, "Scheibe", "Brot");
        assertParsed(FoodTextParser.parse("1 Stk. Kuchen").getFirst(), 1.0, "Stück", "Kuchen");
    }

    @Test
    void testFractionsAndMultipliers() {
        assertParsed(FoodTextParser.parse("½ Avocado").getFirst(), 0.5, null, "Avocado");
        assertParsed(FoodTextParser.parse("ein halbes Brötchen").getFirst(), 0.5, null, "Brötchen");
        assertParsed(FoodTextParser.parse("2x Toast").getFirst(), 2.0, null, "Toast");
        assertParsed(FoodTextParser.parse("drei Scheiben von Käse").getFirst(), 3.0, "Scheibe", "Käse");
    }

    @Test
    void testPercentInNameDoesNotStartNewItem() {
        List<ParsedFood> parsed = FoodTextParser.parse("200 g Joghurt 3,5 % Fett");

        assertEquals(1, parsed.size());
        assertParsed(parsed.getFirst(), 200.0, "g", "Joghurt 3,5 % Fett");
    }

    @Test
    void testUnitWithoutNameIsTheName() {
        assertParsed(FoodTextParser.parse("1 Riegel").getFirst(), 1.0, null, "Riegel");
    }

    @Test
    void testEmptyText() {
        assertTrue(FoodTextParser.parse("").isEmpty());
        assertTrue(FoodTextParser.parse("  ,  und ").isEmpty());
        assertTrue(FoodTextParser.parse(null).isEmpty());
    }

    @Test
    void testNameVariants() {
        assertEquals("eier", FoodTextParser.nameVariants("Eier").getFirst());
        assertTrue(FoodTextParser.nameVariants("Eier").contains("ei"));
        assertTrue(FoodTextParser.nameVariants("Ei").contains("eier"));
        assertTrue(FoodTextParser.nameVariants("Bananen").contains("banane"));
        assertTrue(FoodTextParser.nameVariants("Banane").contains("bananen"));
        assertTrue(FoodTextParser.nameVariants("Walnüsse").contains("walnuss"));
        assertEquals(List.of("griechischer joghurt"), FoodTextParser.nameVariants("griechischer Joghurt"));
    }

    @Test
    void testParsedFoodWithoutUnitHasNoUnit() {
        assertNull(FoodTextParser.parse("Apfel").getFirst().unit());
    }
}
