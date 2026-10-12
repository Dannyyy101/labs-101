-- Days a user used the app (see LoginDayRepository), for the login streak on
-- the profile page. One row per user and local day, the background sync of
-- the iOS app doesn't count.
CREATE TABLE IF NOT EXISTS login_day (
    user_id text NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    day     date NOT NULL,
    PRIMARY KEY (user_id, day)
);

-- before this table existed, a day with a meal or a workout was a day in the app
INSERT INTO login_day (user_id, day)
SELECT user_id, (create_date AT TIME ZONE 'Europe/Berlin')::date FROM food_user
WHERE user_id IS NOT NULL AND create_date IS NOT NULL
UNION
SELECT user_id, (started_at AT TIME ZONE 'Europe/Berlin')::date FROM workout_session
ON CONFLICT DO NOTHING;
