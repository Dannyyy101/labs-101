-- The fuzzy food search (FoodRepository#search) calls many functions per row, so
-- Postgres estimates it as expensive and JIT compiles it. That alone takes
-- 10-30 ms, more than the query runs on the few rows it actually scores.
-- Applies to new connections.
DO $$
BEGIN
    EXECUTE format('ALTER DATABASE %I SET jit = off', current_database());
END
$$;
