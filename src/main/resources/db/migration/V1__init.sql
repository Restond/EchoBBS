CREATE TABLE IF NOT EXISTS app_metadata (
    id BIGSERIAL PRIMARY KEY,
    meta_key VARCHAR(64) NOT NULL UNIQUE,
    meta_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO app_metadata (meta_key, meta_value)
VALUES ('schema_initialized', 'true')
ON CONFLICT (meta_key) DO NOTHING;