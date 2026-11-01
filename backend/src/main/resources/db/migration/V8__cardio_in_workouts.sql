-- Templates can contain cardio (CARDIO_EXERCISE). Hibernate created a check
-- constraint with the types of back then and doesn't change it on update, the
-- constraint is dropped and Hibernate's validation of the enum is enough.
-- On a new database the tables don't exist yet, Hibernate creates them with all types.
DO $$
DECLARE
    constraint_name text;
BEGIN
    IF to_regclass('workout_exercise') IS NOT NULL THEN
        FOR constraint_name IN
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
            WHERE c.conrelid = 'workout_exercise'::regclass AND c.contype = 'c' AND a.attname = 'type'
        LOOP
            EXECUTE format('ALTER TABLE workout_exercise DROP CONSTRAINT %I', constraint_name);
        END LOOP;
    END IF;

    -- cardio machines of the gym, a new database gets them from scripts/seed_exercises.sql
    IF to_regclass('exercise') IS NOT NULL THEN
        INSERT INTO exercise (name, description, type)
        SELECT e.name, e.description, 'Cardio'
        FROM (VALUES
            ('Laufband', 'Laufen oder Gehen auf dem Laufband'),
            ('Rudergerät', 'Rudern an der Maschine'),
            ('Fahrradergometer', 'Radfahren auf dem Ergometer'),
            ('Crosstrainer', 'Ganzkörper-Ausdauer am Crosstrainer'),
            ('Stepper', 'Treppensteigen an der Maschine'),
            ('Seilspringen', 'Springseil, locker oder in Intervallen')
        ) AS e (name, description)
        WHERE NOT EXISTS (SELECT 1 FROM exercise x WHERE lower(x.name) = lower(e.name));
    END IF;
END
$$;
