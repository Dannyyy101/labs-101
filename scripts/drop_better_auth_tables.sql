-- Removes what is left of Better Auth, once every user was moved to their
-- Zitadel id (see README, "Umstieg auf Zitadel"). The "user" table stays, the
-- backend keeps its copy of the Zitadel users there.
--
--   docker exec -i postgres-labs-101 psql -U user -d labs101 < scripts/drop_better_auth_tables.sql

BEGIN;

-- users that were never moved still have their Better Auth id (32 letters and
-- digits, zitadel ids are only digits), stop instead of leaving their data behind
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM "user" WHERE id !~ '^[0-9]+$') THEN
        RAISE EXCEPTION 'There are still users with a Better Auth id, move them with move_user first';
    END IF;
END
$$;

DROP TABLE IF EXISTS session;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS verification;

COMMIT;
