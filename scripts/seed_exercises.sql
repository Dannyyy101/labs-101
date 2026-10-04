-- Active: 1787834448744@@127.0.0.1@5432@labs101
-- Grundstock an Übungen mit den trainierten Muskeln für die Muskelkarte.
--
--   docker exec -i postgres-labs-101 psql -U user -d labs101 < scripts/seed_exercises.sql
--
-- Übungen, deren Name es schon gibt, werden übersprungen, Muskeln bekommen nur
-- die neu angelegten Übungen. Das Skript kann also mehrfach laufen.
--
-- Muskeln wie im Übungseditor: Intensität 3 (rot) = Hauptmuskel,
-- Intensität 1 (grün) = unterstützender Muskel. Slugs siehe react-muscle-highlighter.

BEGIN;

WITH exercises (name, description, type) AS (
    VALUES
    -- Brust
    ('Schrägbankdrücken', 'Langhantel auf der Schrägbank (30–45°), betont die obere Brust', 'Strength Training'),
    ('Kurzhantel-Bankdrücken', 'Flachbank mit Kurzhanteln, größerer Bewegungsumfang als mit der Langhantel', 'Strength Training'),
    ('Butterfly', 'An der Maschine oder mit Kurzhanteln, Arme leicht gebeugt zusammenführen', 'Strength Training'),
    ('Kabelzug-Crossover', 'Kabel von oben vor dem Körper zusammenführen', 'Strength Training'),
    ('Dips', 'Am Barren, Oberkörper leicht nach vorne für mehr Brust, aufrecht für mehr Trizeps', 'Strength Training'),
    ('Liegestütze', 'Körper gerade halten, Brust bis kurz über den Boden', 'Strength Training'),

-- Rücken
(
    'Klimmzüge',
    'Obergriff, schulterbreit, Kinn über die Stange',
    'Strength Training'
),
(
    'Latziehen',
    'Stange zur oberen Brust ziehen, Schulterblätter nach unten',
    'Strength Training'
),
(
    'Langhantelrudern',
    'Oberkörper vorgebeugt, Stange zum Bauchnabel ziehen',
    'Strength Training'
),
(
    'Kabelrudern sitzend',
    'Enger Griff, Griff zum Bauch ziehen, Rücken gerade',
    'Strength Training'
),
(
    'Kurzhantelrudern einarmig',
    'Auf der Bank abgestützt, Hantel zur Hüfte ziehen',
    'Strength Training'
),
(
    'Kreuzheben',
    'Langhantel vom Boden, Rücken neutral, Hüfte und Knie gleichzeitig strecken',
    'Strength Training'
),
(
    'Hyperextensions',
    'Rückenstrecker an der Hyperextension-Bank',
    'Strength Training'
),
(
    'Face Pulls',
    'Seil am Kabel auf Gesichtshöhe ziehen, Ellbogen hoch',
    'Strength Training'
),

-- Schultern
(
    'Schulterdrücken',
    'Stehend oder sitzend mit Lang- oder Kurzhanteln über Kopf drücken',
    'Strength Training'
),
(
    'Seitheben',
    'Kurzhanteln seitlich bis Schulterhöhe heben',
    'Strength Training'
),
(
    'Reverse Flys',
    'Vorgebeugt oder an der Maschine, hintere Schulter',
    'Strength Training'
),
(
    'Shrugs',
    'Schultern mit schweren Hanteln Richtung Ohren ziehen',
    'Strength Training'
),

-- Arme
(
    'Langhantel-Curls',
    'Stehend, Ellbogen am Körper',
    'Strength Training'
),
(
    'Hammer Curls',
    'Kurzhanteln im neutralen Griff curlen',
    'Strength Training'
),
(
    'Trizepsdrücken am Kabel',
    'Seil oder Stange nach unten drücken, Ellbogen fixiert',
    'Strength Training'
),
(
    'French Press',
    'SZ-Stange liegend zur Stirn absenken',
    'Strength Training'
),
(
    'Unterarmcurls',
    'Unterarme auf der Bank, Handgelenke beugen',
    'Strength Training'
),

-- Beine
(
    'Kniebeugen',
    'Langhantel auf dem oberen Rücken, mindestens bis parallel',
    'Strength Training'
),
(
    'Frontkniebeugen',
    'Langhantel vorne auf den Schultern, Oberkörper aufrecht',
    'Strength Training'
),
(
    'Beinpresse',
    'Füße schulterbreit, Knie nicht ganz durchstrecken',
    'Strength Training'
),
(
    'Ausfallschritte',
    'Mit Kurzhanteln, abwechselnd oder gehend',
    'Strength Training'
),
(
    'Bulgarian Split Squats',
    'Hinterer Fuß auf der Bank, einbeinige Kniebeuge',
    'Strength Training'
),
(
    'Rumänisches Kreuzheben',
    'Knie leicht gebeugt, Hüfte nach hinten schieben, Dehnung im Beinbeuger',
    'Strength Training'
),
(
    'Beinstrecker',
    'Maschine, Quadrizeps isoliert',
    'Strength Training'
),
(
    'Beinbeuger liegend',
    'Maschine, Fersen Richtung Gesäß',
    'Strength Training'
),
(
    'Hip Thrusts',
    'Oberer Rücken auf der Bank, Langhantel auf der Hüfte, Hüfte strecken',
    'Strength Training'
),
(
    'Wadenheben stehend',
    'Auf der Kante stehend, volle Dehnung und auf die Zehenspitzen',
    'Strength Training'
),
(
    'Adduktorenmaschine',
    'Beine gegen den Widerstand zusammendrücken',
    'Strength Training'
),

-- Rumpf
(
    'Plank',
    'Unterarmstütz, Körper gerade, auf Zeit (Wdh = Sekunden)',
    'Strength Training'
),
(
    'Seitstütz',
    'Seitlicher Unterarmstütz, auf Zeit (Wdh = Sekunden)',
    'Strength Training'
),
(
    'Crunches',
    'Oberkörper einrollen, unterer Rücken bleibt am Boden',
    'Strength Training'
),
(
    'Beinheben hängend',
    'An der Stange hängend Beine bzw. Knie anheben',
    'Strength Training'
),
(
    'Russian Twists',
    'Sitzend, Oberkörper zurückgelehnt, abwechselnd zur Seite drehen',
    'Strength Training'
),

-- Laufen
(
    'Lockerer Dauerlauf',
    'Ruhiges Tempo in Zone 2',
    'Running'
),
(
    'Intervalllauf',
    'Schnelle Abschnitte mit Trabpausen, z. B. 6 × 800 m',
    'Running'
),
(
    'Tempodauerlauf',
    '20–40 min an der Schwelle',
    'Running'
),

-- Schwimmen
(
    'Kraulschwimmen',
    'Freistil',
    'Swimming'
),
(
    'Brustschwimmen',
    'Brustbeinschlag mit Armzug',
    'Swimming'
),

-- Dehnen
('Oberschenkel-Dehnung', 'Stehend, Ferse zum Gesäß ziehen', 'Stretching'),
    ('Beinbeuger-Dehnung', 'Bein gestreckt auflegen, Oberkörper nach vorne', 'Stretching'),
    ('Brust-Dehnung', 'Unterarm am Türrahmen, Oberkörper wegdrehen', 'Stretching'),
    ('Waden-Dehnung', 'An der Wand, hinteres Bein gestreckt, Ferse am Boden', 'Stretching')
),
inserted AS (
    INSERT INTO exercise (name, description, type)
    SELECT e.name, e.description, e.type
    FROM exercises e
    WHERE NOT EXISTS (SELECT 1 FROM exercise x WHERE lower(x.name) = lower(e.name))
    RETURNING id, name
),
muscles (exercise, slug, intensity) AS (
    VALUES
    ('Schrägbankdrücken', 'chest', 3), ('Schrägbankdrücken', 'deltoids', 1), ('Schrägbankdrücken', 'triceps', 1),
    ('Kurzhantel-Bankdrücken', 'chest', 3), ('Kurzhantel-Bankdrücken', 'triceps', 1), ('Kurzhantel-Bankdrücken', 'deltoids', 1),
    ('Butterfly', 'chest', 3), ('Butterfly', 'deltoids', 1),
    ('Kabelzug-Crossover', 'chest', 3), ('Kabelzug-Crossover', 'deltoids', 1),
    ('Dips', 'triceps', 3), ('Dips', 'chest', 3), ('Dips', 'deltoids', 1),
    ('Liegestütze', 'chest', 3), ('Liegestütze', 'triceps', 1), ('Liegestütze', 'deltoids', 1), ('Liegestütze', 'abs', 1),

    ('Klimmzüge', 'upper-back', 3), ('Klimmzüge', 'biceps', 1), ('Klimmzüge', 'forearm', 1),
    ('Latziehen', 'upper-back', 3), ('Latziehen', 'biceps', 1),
    ('Langhantelrudern', 'upper-back', 3), ('Langhantelrudern', 'trapezius', 1), ('Langhantelrudern', 'lower-back', 1), ('Langhantelrudern', 'biceps', 1),
    ('Kabelrudern sitzend', 'upper-back', 3), ('Kabelrudern sitzend', 'trapezius', 1), ('Kabelrudern sitzend', 'biceps', 1),
    ('Kurzhantelrudern einarmig', 'upper-back', 3), ('Kurzhantelrudern einarmig', 'biceps', 1),
    ('Kreuzheben', 'lower-back', 3), ('Kreuzheben', 'gluteal', 3), ('Kreuzheben', 'hamstring', 3), ('Kreuzheben', 'trapezius', 1), ('Kreuzheben', 'quadriceps', 1), ('Kreuzheben', 'forearm', 1),
    ('Hyperextensions', 'lower-back', 3), ('Hyperextensions', 'gluteal', 1), ('Hyperextensions', 'hamstring', 1),
    ('Face Pulls', 'deltoids', 3), ('Face Pulls', 'trapezius', 1), ('Face Pulls', 'upper-back', 1),

    ('Schulterdrücken', 'deltoids', 3), ('Schulterdrücken', 'triceps', 1), ('Schulterdrücken', 'trapezius', 1),
    ('Seitheben', 'deltoids', 3), ('Seitheben', 'trapezius', 1),
    ('Reverse Flys', 'deltoids', 3), ('Reverse Flys', 'upper-back', 1),
    ('Shrugs', 'trapezius', 3), ('Shrugs', 'forearm', 1),

    ('Langhantel-Curls', 'biceps', 3), ('Langhantel-Curls', 'forearm', 1),
    ('Hammer Curls', 'biceps', 3), ('Hammer Curls', 'forearm', 3),
    ('Trizepsdrücken am Kabel', 'triceps', 3),
    ('French Press', 'triceps', 3),
    ('Unterarmcurls', 'forearm', 3),

    ('Kniebeugen', 'quadriceps', 3), ('Kniebeugen', 'gluteal', 3), ('Kniebeugen', 'hamstring', 1), ('Kniebeugen', 'adductors', 1), ('Kniebeugen', 'lower-back', 1),
    ('Frontkniebeugen', 'quadriceps', 3), ('Frontkniebeugen', 'gluteal', 1), ('Frontkniebeugen', 'abs', 1),
    ('Beinpresse', 'quadriceps', 3), ('Beinpresse', 'gluteal', 1), ('Beinpresse', 'hamstring', 1),
    ('Ausfallschritte', 'quadriceps', 3), ('Ausfallschritte', 'gluteal', 3), ('Ausfallschritte', 'hamstring', 1), ('Ausfallschritte', 'adductors', 1),
    ('Bulgarian Split Squats', 'quadriceps', 3), ('Bulgarian Split Squats', 'gluteal', 3), ('Bulgarian Split Squats', 'adductors', 1),
    ('Rumänisches Kreuzheben', 'hamstring', 3), ('Rumänisches Kreuzheben', 'gluteal', 3), ('Rumänisches Kreuzheben', 'lower-back', 1),
    ('Beinstrecker', 'quadriceps', 3),
    ('Beinbeuger liegend', 'hamstring', 3), ('Beinbeuger liegend', 'calves', 1),
    ('Hip Thrusts', 'gluteal', 3), ('Hip Thrusts', 'hamstring', 1),
    ('Wadenheben stehend', 'calves', 3),
    ('Adduktorenmaschine', 'adductors', 3),

    ('Plank', 'abs', 3), ('Plank', 'obliques', 1), ('Plank', 'lower-back', 1),
    ('Seitstütz', 'obliques', 3), ('Seitstütz', 'abs', 1),
    ('Crunches', 'abs', 3),
    ('Beinheben hängend', 'abs', 3), ('Beinheben hängend', 'obliques', 1), ('Beinheben hängend', 'forearm', 1),
    ('Russian Twists', 'obliques', 3), ('Russian Twists', 'abs', 1),

    ('Lockerer Dauerlauf', 'calves', 3), ('Lockerer Dauerlauf', 'quadriceps', 1), ('Lockerer Dauerlauf', 'hamstring', 1), ('Lockerer Dauerlauf', 'gluteal', 1),
    ('Intervalllauf', 'quadriceps', 3), ('Intervalllauf', 'calves', 3), ('Intervalllauf', 'hamstring', 1), ('Intervalllauf', 'gluteal', 1),
    ('Tempodauerlauf', 'quadriceps', 3), ('Tempodauerlauf', 'calves', 3), ('Tempodauerlauf', 'hamstring', 1), ('Tempodauerlauf', 'gluteal', 1),

    ('Kraulschwimmen', 'upper-back', 3), ('Kraulschwimmen', 'deltoids', 3), ('Kraulschwimmen', 'triceps', 1), ('Kraulschwimmen', 'abs', 1),
    ('Brustschwimmen', 'chest', 3), ('Brustschwimmen', 'adductors', 3), ('Brustschwimmen', 'upper-back', 1), ('Brustschwimmen', 'quadriceps', 1),

    ('Oberschenkel-Dehnung', 'quadriceps', 1),
    ('Beinbeuger-Dehnung', 'hamstring', 1),
    ('Brust-Dehnung', 'chest', 1), ('Brust-Dehnung', 'deltoids', 1),
    ('Waden-Dehnung', 'calves', 1)
)
INSERT INTO body_part (exercise_id, slug, intensity, color)
SELECT i.id, m.slug, m.intensity, CASE m.intensity WHEN 3 THEN '#ff0000' ELSE '#00ff00' END
FROM muscles m
JOIN inserted i ON i.name = m.exercise;

COMMIT;