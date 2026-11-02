CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_post_search ON post USING GIN ((title || ' ' || content) gin_trgm_ops);

CREATE INDEX idx_post_created_at ON post(created_at DESC) WHERE status = 'NORMAL';