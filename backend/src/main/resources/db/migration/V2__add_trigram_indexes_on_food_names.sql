-- Trigram indexes for the fuzzy name search in FoodRepository/OpenFoodRepository.
-- Both queries filter on lower(name), so the index has to be on that expression.

-- some databases already have this index under a hand written name
ALTER INDEX IF EXISTS gericht_name_trgm RENAME TO idx_open_food_name_trgm;

CREATE INDEX IF NOT EXISTS idx_open_food_name_trgm ON open_food USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_food_name_trgm ON food USING gin (lower(name) gin_trgm_ops);
