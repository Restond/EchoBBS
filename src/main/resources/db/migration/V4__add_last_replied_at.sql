ALTER TABLE post ADD COLUMN last_replied_at TIMESTAMP NOT NULL DEFAULT NOW();
UPDATE post SET last_replied_at = created_at;
