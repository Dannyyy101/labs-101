package com.labs_101.backend.foodExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses free text like "2 Eier, eine Tasse Cappuccino, 1 Banane und 30 g
 * Mandeln" into amount, unit and food name.
 */
public final class FoodTextParser {

    public record ParsedFood(Double amount, String unit, String name) {
    }

    // splits on "," ";" "+" "&" newlines and the words "und"/"sowie", but keeps
    // decimal commas like "1,5"
    private static final Pattern SEPARATOR = Pattern.compile(
            "(?<!\\d),|,(?!\\d)|;|\\+|&|\\n|(?iu)\\b(?:und|sowie)\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern NUMBER_FOLLOWED_BY_TEXT = Pattern.compile("(\\d)(\\p{L})");
    private static final Pattern MULTIPLIER = Pattern.compile("(?i)(\\d)\\s*x\\b");

    private static final Map<String, Double> WORD_NUMBERS = Map.ofEntries(
            Map.entry("ein", 1.0), Map.entry("eine", 1.0), Map.entry("einen", 1.0), Map.entry("einem", 1.0),
            Map.entry("einer", 1.0), Map.entry("eins", 1.0), Map.entry("zwei", 2.0), Map.entry("drei", 3.0),
            Map.entry("vier", 4.0), Map.entry("fünf", 5.0), Map.entry("sechs", 6.0), Map.entry("sieben", 7.0),
            Map.entry("acht", 8.0), Map.entry("neun", 9.0), Map.entry("zehn", 10.0), Map.entry("elf", 11.0),
            Map.entry("zwölf", 12.0), Map.entry("halb", 0.5), Map.entry("halbe", 0.5), Map.entry("halber", 0.5),
            Map.entry("halbes", 0.5), Map.entry("halben", 0.5), Map.entry("anderthalb", 1.5),
            Map.entry("½", 0.5), Map.entry("¼", 0.25), Map.entry("¾", 0.75));

    private record Unit(String label, double factor) {
    }

    // lowercase spelling -> canonical label (and factor to convert the amount)
    private static final Map<String, Unit> UNITS = buildUnits();

    private static final Set<String> FILLER_WORDS = Set.of("von", "vom", "voll");

    private FoodTextParser() {
    }

    public static List<ParsedFood> parse(String text) {
        if (text == null || text.isBlank())
            return List.of();

        String normalized = MULTIPLIER.matcher(text).replaceAll("$1");
        normalized = NUMBER_FOLLOWED_BY_TEXT.matcher(normalized).replaceAll("$1 $2");

        List<ParsedFood> result = new ArrayList<>();
        for (String segment : SEPARATOR.split(normalized)) {
            for (List<String> item : splitByAmounts(tokenize(segment))) {
                ParsedFood parsed = parseItem(item);
                if (parsed != null)
                    result.add(parsed);
            }
        }
        return result;
    }

    /**
     * Spelling variants of a food name (singular/plural) to improve matching,
     * e.g. "Eier" -> [eier, ei], "Banane" -> [banane, bananen]. The original
     * name always comes first.
     */
    public static List<String> nameVariants(String name) {
        String q = name.toLowerCase().trim();
        Set<String> variants = new LinkedHashSet<>();
        variants.add(q);
        if (q.contains(" "))
            return List.copyOf(variants);

        for (String suffix : List.of("en", "er", "n", "e", "s")) {
            if (q.endsWith(suffix) && q.length() - suffix.length() >= 2) {
                String singular = q.substring(0, q.length() - suffix.length());
                variants.add(singular);
                // "walnüsse" -> "walnuss", "äpfel" -> "apfel"
                variants.add(withoutUmlauts(singular));
            }
        }
        if (!q.equals(withoutUmlauts(q)))
            variants.add(withoutUmlauts(q));
        if (q.length() <= 3)
            variants.add(q + "er");
        if (q.endsWith("e"))
            variants.add(q + "n");
        return List.copyOf(variants);
    }

    private static String withoutUmlauts(String word) {
        return word.replace("ä", "a").replace("ö", "o").replace("ü", "u");
    }

    private static List<String> tokenize(String segment) {
        return Arrays.stream(segment.trim().split("\\s+"))
                .map((token) -> token.replaceAll("^[\"'„“(\\[]+|[\"'“”)\\].!?:]+$", ""))
                .filter((token) -> !token.isEmpty())
                .toList();
    }

    // "eine Banane ein Apfel" -> [eine Banane], [ein Apfel]
    private static List<List<String>> splitByAmounts(List<String> tokens) {
        List<List<String>> items = new ArrayList<>();
        List<String> current = new ArrayList<>();
        boolean hasName = false;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            boolean startsNewItem = hasName && isAmount(token)
                    && i + 1 < tokens.size() && !tokens.get(i + 1).equals("%");
            if (startsNewItem) {
                items.add(current);
                current = new ArrayList<>();
                hasName = false;
            }
            current.add(token);
            if (!isAmount(token) && !UNITS.containsKey(token.toLowerCase()))
                hasName = true;
        }
        if (!current.isEmpty())
            items.add(current);
        return items;
    }

    private static ParsedFood parseItem(List<String> tokens) {
        int i = 0;
        Double amount = null;

        if (i < tokens.size() && isAmount(tokens.get(i))) {
            amount = toAmount(tokens.get(i));
            i++;
            // "ein halbes Brötchen" -> 0.5
            Double next = i < tokens.size() - 1 ? WORD_NUMBERS.get(tokens.get(i).toLowerCase()) : null;
            if (amount == 1.0 && next != null && next == 0.5) {
                amount = 0.5;
                i++;
            }
        }

        String unit = null;
        if (i < tokens.size() - 1) {
            Unit found = UNITS.get(tokens.get(i).toLowerCase());
            if (found != null) {
                unit = found.label();
                if (amount != null)
                    amount *= found.factor();
                i++;
            }
        }

        while (i < tokens.size() - 1 && FILLER_WORDS.contains(tokens.get(i).toLowerCase()))
            i++;

        String name = String.join(" ", tokens.subList(i, tokens.size())).trim();
        if (name.isEmpty())
            return null;
        return new ParsedFood(amount, unit, name);
    }

    private static boolean isAmount(String token) {
        return NUMBER.matcher(token).matches() || WORD_NUMBERS.containsKey(token.toLowerCase());
    }

    private static Double toAmount(String token) {
        Double word = WORD_NUMBERS.get(token.toLowerCase());
        return word != null ? word : Double.valueOf(token.replace(',', '.'));
    }

    private static Map<String, Unit> buildUnits() {
        Map<String, Unit> units = new java.util.HashMap<>();
        addUnit(units, "g", 1, "g", "gr", "gramm");
        addUnit(units, "g", 1000, "kg", "kilo", "kilogramm");
        addUnit(units, "ml", 1, "ml", "milliliter");
        addUnit(units, "ml", 10, "cl");
        addUnit(units, "ml", 1000, "l", "liter");
        addUnit(units, "Esslöffel", 1, "el", "esslöffel");
        addUnit(units, "Teelöffel", 1, "tl", "teelöffel");
        addUnit(units, "Tasse", 1, "tasse", "tassen");
        addUnit(units, "Glas", 1, "glas", "gläser");
        addUnit(units, "Becher", 1, "becher");
        addUnit(units, "Scheibe", 1, "scheibe", "scheiben");
        addUnit(units, "Stück", 1, "stück", "stücke", "stk");
        addUnit(units, "Schüssel", 1, "schüssel", "schüsseln");
        addUnit(units, "Schale", 1, "schale", "schalen");
        addUnit(units, "Portion", 1, "portion", "portionen");
        addUnit(units, "Handvoll", 1, "handvoll");
        addUnit(units, "Prise", 1, "prise", "prisen");
        addUnit(units, "Dose", 1, "dose", "dosen");
        addUnit(units, "Flasche", 1, "flasche", "flaschen");
        addUnit(units, "Packung", 1, "packung", "packungen", "pkg", "pck");
        addUnit(units, "Riegel", 1, "riegel");
        addUnit(units, "Kugel", 1, "kugel", "kugeln");
        addUnit(units, "Zehe", 1, "zehe", "zehen");
        addUnit(units, "Stange", 1, "stange", "stangen");
        return Map.copyOf(units);
    }

    private static void addUnit(Map<String, Unit> units, String label, double factor, String... spellings) {
        for (String spelling : spellings)
            units.put(spelling, new Unit(label, factor));
    }
}
