package com.labs_101.backend.foodExtractor;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.food.ExtractedFoodDto;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.OpenFood;
import com.labs_101.backend.foodExtractor.FoodTextParser.ParsedFood;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodMatchProjection;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.OpenFoodRepository;

/**
 * Turns free text into foods. Every item is matched against our own foods
 * (BLS and already imported ones) and the open food database, the best match
 * wins. Open foods are only returned as preview here, they are copied into
 * {@code food} once they get tracked.
 */
@Service
public class FoodExtractor {
    /** matches below this score are treated as not found */
    static final double MIN_SCORE = 0.3;
    /** singular/plural variants should only win if they are clearly better */
    static final double VARIANT_PENALTY = 0.05;
    /** our own foods are more reliable than open food entries */
    static final double FOOD_BONUS = 0.15;

    private final FoodRepository foodRepository;
    private final OpenFoodRepository openFoodRepository;
    private final FoodMapper foodMapper;

    public FoodExtractor(FoodRepository foodRepository, OpenFoodRepository openFoodRepository,
            FoodMapper foodMapper) {
        this.foodRepository = foodRepository;
        this.openFoodRepository = openFoodRepository;
        this.foodMapper = foodMapper;
    }

    public record Match(Food food, OpenFood openFood, double score) {
    }

    @Transactional(readOnly = true)
    public List<ExtractedFoodDto> extract(String text) {
        return FoodTextParser.parse(text).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Match> findBestMatch(String name) {
        List<String> variants = FoodTextParser.nameVariants(name);

        FoodMatchProjection bestFood = null;
        double bestFoodScore = Double.NEGATIVE_INFINITY;
        boolean strongFoodMatch = false;
        for (int i = 0; i < variants.size(); i++) {
            String variant = variants.get(i);
            Optional<FoodMatchProjection> match = foodRepository.findBestMatch(variant);
            if (match.isEmpty())
                continue;
            double score = match.get().getScore() - (i == 0 ? 0 : VARIANT_PENALTY);
            if (score > bestFoodScore) {
                bestFood = match.get();
                bestFoodScore = score;
                strongFoodMatch = startsWith(bestFood.getName(), variant);
            }
        }

        // e.g. "Banane" -> "Banane roh", no need to search the open food database
        if (bestFood != null && strongFoodMatch)
            return Optional.of(new Match(foodRepository.getReferenceById(bestFood.getId()), null, bestFoodScore));

        Optional<FoodMatchProjection> bestOpenFood = openFoodRepository.findBestMatch(variants.getFirst());
        boolean openFoodWins = bestOpenFood.isPresent()
                && (bestFood == null || bestOpenFood.get().getScore() > bestFoodScore + FOOD_BONUS);

        if (openFoodWins) {
            if (bestOpenFood.get().getScore() < MIN_SCORE)
                return Optional.empty();
            OpenFood openFood = openFoodRepository.getReferenceById(bestOpenFood.get().getId());
            // already imported before
            Optional<Food> imported = openFood.getBarCode() == null ? Optional.empty()
                    : foodRepository.findByBarCode(openFood.getBarCode());
            return Optional.of(imported
                    .map((food) -> new Match(food, null, bestOpenFood.get().getScore()))
                    .orElse(new Match(null, openFood, bestOpenFood.get().getScore())));
        }

        if (bestFood == null || bestFoodScore < MIN_SCORE)
            return Optional.empty();
        return Optional.of(new Match(foodRepository.getReferenceById(bestFood.getId()), null, bestFoodScore));
    }

    private ExtractedFoodDto toDto(ParsedFood parsed) {
        Optional<Match> match = findBestMatch(parsed.name());
        Double amount = parsed.amount() != null ? parsed.amount() : 1.0;

        if (match.isEmpty())
            return new ExtractedFoodDto(parsed.name(), amount, parsed.unit(), null, null);
        if (match.get().openFood() != null) {
            OpenFood openFood = match.get().openFood();
            return new ExtractedFoodDto(parsed.name(), amount, parsed.unit(),
                    FoodMapper.mapFromOpenFoodToFoodWithPortionsDto(openFood), openFood.getId());
        }
        return new ExtractedFoodDto(parsed.name(), amount, parsed.unit(),
                foodMapper.mapFromEntityToFoodWithPortionsDto(match.get().food()), null);
    }

    /**
     * Same rule as the first word bonus in {@link FoodMatchProjection#SCORE}:
     * "Banane roh" starts with "banane", "Hähnchen Brust, roh" with
     * "hähnchenbrust".
     */
    static boolean startsWith(String name, String query) {
        String[] words = name.toLowerCase().replaceAll("\\([^)]*\\)", " ").replaceAll(",.*$", "").trim()
                .split("\\s+");
        return words[0].equals(query)
                || (words.length > 1 && (words[0] + words[1]).equals(query))
                || String.join("", words).equals(query);
    }
}
