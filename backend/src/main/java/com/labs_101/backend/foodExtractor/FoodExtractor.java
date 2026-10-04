package com.labs_101.backend.foodExtractor;

import com.labs_101.backend.dtos.food.FoodDto;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Service
public class FoodExtractor {
    private final FoodRepository foodRepository;
    private final FoodMapper foodMapper;
    private String text;
    private final static HashMap<String, Double> wordNumbersMapping = new HashMap<>(initWordNumberMapping());

    public FoodExtractor(FoodRepository foodRepository, FoodMapper foodMapper) {
        this.foodRepository = foodRepository;
        this.foodMapper = foodMapper;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public class FoodPrompt {
        private Double amount;
        private String portion = "g";
        private String name;
    }

    public List<TrackedFoodDto> extractFood(String text) {
        List<FoodPrompt> elements = splitElements(text);
        return elements.stream().map((element) -> {
            Food food = getFoodByName(element.getName());
            Optional<FoodPortion> foundPortion = food.getPortions().stream()
                    .filter((portion) -> portion.getLabel().equals(element.getPortion()))
                    .findFirst();
            FoodDto dto = foodMapper.mapFromEntityToFoodDto(food);

            return new TrackedFoodDto(null, dto, element.getAmount(), null,
                    FoodMapper.mapFromFoodPortionToFoodPortionDto(foundPortion.orElse(null)));
        }).toList();
    }

    public Food getFoodByName(String name) {
        return foodRepository.findSimilar(name);
    }

    public List<FoodPrompt> splitElements(String text) {
        String[] words = text.split(" ");
        ArrayList<FoodPrompt> foods = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            FoodPrompt current = new FoodPrompt();
            Double mapping = wordNumbersMapping.get(words[i]);

            if (mapping != null || isNumeric(words[i])) {
                if (mapping != null) {
                    current.setAmount(mapping);
                } else {
                    current.setAmount(Double.valueOf(words[i]));
                }
            }
            i += 1;
            String portion = words[i];

            if (i + 1 >= words.length || wordNumbersMapping.containsKey(words[i + 1]) || isNumeric(words[i + 1])) {
                current.setName(portion);
                foods.add(current);
                continue;
            }
            i += 1;
            current.setPortion(portion);
            current.setName(words[i]);
            foods.add(current);
        }

        return foods;
    }

    private static HashMap<String, Double> initWordNumberMapping() {
        HashMap<String, Double> wordNumbersMapping = new HashMap<>();
        wordNumbersMapping.put("ein", 1.0);
        wordNumbersMapping.put("eine", 1.0);
        wordNumbersMapping.put("zwei", 2.0);
        wordNumbersMapping.put("drei", 3.0);
        return wordNumbersMapping;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
