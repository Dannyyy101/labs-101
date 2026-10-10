-- The users live in Zitadel now, the backend creates the row of a user on
-- their first request (UserProvisioning). Better Auth made the email unique,
-- but a Zitadel user can sign in before their old Better Auth user was moved
-- with move_user below, both rows then have the same email for a moment.
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
        WHERE c.conrelid = '"user"'::regclass AND c.contype = 'u' AND a.attname = 'email'
    LOOP
        EXECUTE format('ALTER TABLE "user" DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END
$$;

-- Moves everything of a user to another id, used once per user to move the
-- data of the old Better Auth id to the id of the user in Zitadel:
--   SELECT move_user('<better auth id>', '<zitadel user id>');
-- Follows every foreign key to "user", so new tables are covered without
-- touching this function. Creates the new user as copy of the old one, if they
-- haven't signed in yet.
CREATE OR REPLACE FUNCTION move_user(old_id text, new_id text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    fk record;
BEGIN
    IF old_id = new_id THEN
        RETURN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM "user" WHERE id = old_id) THEN
        RAISE EXCEPTION 'User % not found', old_id;
    END IF;

    INSERT INTO "user" (id, name, email, "emailVerified", image, "createdAt", "updatedAt")
    SELECT new_id, name, email, "emailVerified", image, "createdAt", now() FROM "user" WHERE id = old_id
    ON CONFLICT (id) DO NOTHING;

    FOR fk IN
        SELECT c.conrelid::regclass AS table_name, a.attname AS column_name
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
        WHERE c.contype = 'f' AND c.confrelid = '"user"'::regclass AND cardinality(c.conkey) = 1
    LOOP
        EXECUTE format('UPDATE %s SET %I = $1 WHERE %I = $2', fk.table_name, fk.column_name, fk.column_name)
            USING new_id, old_id;
    END LOOP;

    DELETE FROM "user" WHERE id = old_id;
END
$$;
