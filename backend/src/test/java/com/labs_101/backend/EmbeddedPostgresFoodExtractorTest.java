package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.food.ExtractedFoodDto;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.entities.food.OpenFood;
import com.labs_101.backend.foodExtractor.FoodExtractor;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.OpenFoodRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ FoodExtractor.class, FoodMapper.class })
public class EmbeddedPostgresFoodExtractorTest {
    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private OpenFoodRepository openFoodRepository;

    @Autowired
    private FoodExtractor foodExtractor;

    private Food food(String name) {
        Food food = new Food(name);
        food.setKcal(100.0);
        return foodRepository.save(food);
    }

    private OpenFood openFood(String barCode, String name, Double kcal) {
        return openFoodRepository.save(
                new OpenFood(null, barCode, name, kcal, null, null, null, null, null, null, null, null));
    }

    private String bestMatchName(String query) {
        return foodExtractor.findBestMatch(query)
                .map((match) -> match.food() != null ? match.food().getName() : match.openFood().getName())
                .orElse(null);
    }

    @Test
    void testExtractAmountUnitAndFood() {
        food("Apfel roh");
        Food cappuccino = new Food("Cappuccino (Getränk) mit Milch 3,5 % Fett");
        FoodPortion portion = new FoodPortion();
        portion.setLabel("Tasse");
        portion.setGrams(200.0);
        cappuccino.addPortion(portion);
        foodRepository.save(cappuccino);

        List<ExtractedFoodDto> found = foodExtractor.extract("100 g Apfel, eine Tasse Cappuccino");

        assertEquals(2, found.size());
        assertEquals(100, found.get(0).amount());
        assertEquals("g", found.get(0).unit());
        assertEquals("Apfel roh", found.get(0).food().name());

        assertEquals(1, found.get(1).amount());
        assertEquals("Tasse", found.get(1).unit());
        assertEquals(cappuccino.getId(), found.get(1).food().id());
        assertEquals("Tasse", found.get(1).food().portions().getFirst().label());
        assertNull(found.get(1).openFoodId());
    }

    @Test
    void testUnknownFoodHasNoMatch() {
        food("Apfel roh");

        ExtractedFoodDto found = foodExtractor.extract("2 Zzyzx").getFirst();

        assertEquals("Zzyzx", found.query());
        assertEquals(2, found.amount());
        assertNull(found.food());
        assertNull(found.openFoodId());
    }

    @Test
    void testPrefersPlainFoodOverDishes() {
        food("Bananennektar");
        food("Milchreis mit Milch 3,5 % Fett und Banane roh");
        food("Banane getrocknet");
        food("Banane roh");

        assertEquals("Banane roh", bestMatchName("Banane"));
    }

    @Test
    void testFirstWordMatchBeatsCompoundWords() {
        food("Haferflocken-Nussplätzchen");
        food("Haferflocken ungesüßt, mit Sojadrink und Fruchtmischung");

        assertEquals("Haferflocken ungesüßt, mit Sojadrink und Fruchtmischung", bestMatchName("Haferflocken"));
    }

    @Test
    void testMatchesSingularAndPlural() {
        food("Ei-Einlauf, Suppeneinlage");
        food("Eier gekocht");
        food("Mandel süß");

        assertEquals("Eier gekocht", bestMatchName("Ei"));
        assertEquals("Eier gekocht", bestMatchName("Eier"));
        assertEquals("Mandel süß", bestMatchName("Mandeln"));
    }

    @Test
    void testMatchesUmlautPlural() {
        food("Walnussöl");
        food("Walnuss");

        assertEquals("Walnuss", bestMatchName("Walnüsse"));
    }

    @Test
    void testMatchesCompoundWordsWrittenApart() {
        food("Haferflocken ungesüßt, mit Sojadrink und Fruchtmischung");
        food("Hafer Flocken");
        food("Hähnchen Herz, roh");
        food("Hähnchen Brust, ohne Haut, roh");

        assertEquals("Hafer Flocken", bestMatchName("Haferflocken"));
        assertEquals("Hähnchen Brust, ohne Haut, roh", bestMatchName("Hähnchenbrust"));
    }

    @Test
    void testPrefersUnprocessedFood() {
        food("Reis Mehl");
        food("Reis poliert, roh");
        food("Cappuccino Instantpulver, gesüßt");
        food("Cappuccino (Getränk) mit Milch 3,5 % Fett");

        assertEquals("Reis poliert, roh", bestMatchName("Reis"));
        assertEquals("Cappuccino (Getränk) mit Milch 3,5 % Fett", bestMatchName("Cappuccino"));
        assertEquals("Reis Mehl", bestMatchName("Reis Mehl"));
    }

    @Test
    void testFindsOpenFoodIfNotInFoods() {
        food("Nuss-Nougat-Creme");
        OpenFood nutella = openFood("3017620422003", "Nutella", 539.0);

        ExtractedFoodDto found = foodExtractor.extract("20 g Nutella").getFirst();

        assertEquals(nutella.getId(), found.openFoodId());
        assertNull(found.food().id());
        assertEquals("Nutella", found.food().name());
        assertEquals(539.0, found.food().kcal());
    }

    @Test
    void testPrefersFoodOverOpenFoodOnStrongMatch() {
        food("Milch fettarm, frisch, 1,5 % Fett, pasteurisiert");
        openFood("4000000000001", "Milch", 64.0);

        ExtractedFoodDto found = foodExtractor.extract("200 ml Milch").getFirst();

        assertNull(found.openFoodId());
        assertEquals("Milch fettarm, frisch, 1,5 % Fett, pasteurisiert", found.food().name());
    }

    @Test
    void testPrefersOpenFoodIfClearlyBetter() {
        food("Griechischer Salat");
        openFood("4000000000002", "Griechischer Joghurt", 114.0);

        assertEquals("Griechischer Joghurt", bestMatchName("griechischer Joghurt"));
    }

    @Test
    void testIgnoresOpenFoodWithoutCalories() {
        openFood("4000000000003", "Skyr", null);

        assertTrue(foodExtractor.findBestMatch("Skyr").isEmpty());
    }

    @Test
    void testAlreadyImportedOpenFoodIsReturnedAsFood() {
        Food imported = food("Haselnusscreme Nutella");
        imported.setBarCode("3017620422003");
        foodRepository.save(imported);
        openFood("3017620422003", "Nutella", 539.0);

        ExtractedFoodDto found = foodExtractor.extract("Nutella").getFirst();

        assertNull(found.openFoodId());
        assertNotNull(found.food());
        assertEquals(imported.getId(), found.food().id());
    }
}
