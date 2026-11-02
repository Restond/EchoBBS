CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES "users"(id),
    type VARCHAR(16) NOT NULL CHECK (type IN ('REPLY', 'LIKE')),
    content VARCHAR(512) NOT NULL,
    target_url VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_user_read ON notification (user_id, is_read, created_at DESC);
