CREATE TABLE user_like (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "users"(id),
    target_type VARCHAR(16) NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_user_like_unique ON user_like (user_id, target_type, target_id);
