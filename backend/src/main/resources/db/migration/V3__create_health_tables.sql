-- Apple Health data synced from the iOS app (see HealthController).
-- The HealthKit UUID is the primary key, so uploading the same sample again
-- (e.g. a complete resync after reinstalling the app) only updates the row.
CREATE TABLE IF NOT EXISTS health_sample (
    uuid             uuid PRIMARY KEY,
    user_id          text             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    -- QUANTITY, CATEGORY, WORKOUT, CORRELATION, ECG, AUDIOGRAM, STATE_OF_MIND, SCORED_ASSESSMENT
    kind             varchar(32)      NOT NULL,
    -- HealthKit identifier, e.g. HKQuantityTypeIdentifierHeartRate
    type             varchar(128)     NOT NULL,
    start_date       timestamptz      NOT NULL,
    end_date         timestamptz      NOT NULL,
    -- quantity in `unit`, value of category samples
    value            double precision,
    unit             varchar(64),
    source_name      text,
    source_bundle_id text,
    -- everything else: metadata, device, workout statistics, routes, ...
    payload          jsonb,
    -- set while a complete sync of the type runs, rows of older runs are deleted when it completes
    sync_run         uuid,
    synced_at        timestamptz      NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_health_sample_user_type_start ON health_sample (user_id, type, start_date);
CREATE INDEX IF NOT EXISTS idx_health_sample_user_start ON health_sample (user_id, start_date);

-- date of birth, biological sex, blood type, ... (no samples, they only have a current value)
CREATE TABLE IF NOT EXISTS health_characteristics (
    user_id    text        PRIMARY KEY REFERENCES "user" (id) ON DELETE CASCADE,
    payload    jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Samples created outside of the app (e.g. the website) that the app writes into HealthKit.
-- After writing it acknowledges the request with the UUID HealthKit assigned,
-- the sample then comes back through the normal sync into health_sample.
CREATE TABLE IF NOT EXISTS health_write_request (
    id          bigserial PRIMARY KEY,
    user_id     text             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    kind        varchar(32)      NOT NULL,
    type        varchar(128)     NOT NULL,
    start_date  timestamptz      NOT NULL,
    end_date    timestamptz      NOT NULL,
    value       double precision NOT NULL,
    unit        varchar(64),
    metadata    jsonb,
    -- PENDING, WRITTEN, FAILED
    status      varchar(16)      NOT NULL DEFAULT 'PENDING',
    sample_uuid uuid,
    error       text,
    created_at  timestamptz      NOT NULL DEFAULT now(),
    updated_at  timestamptz      NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_health_write_request_user_status ON health_write_request (user_id, status);
