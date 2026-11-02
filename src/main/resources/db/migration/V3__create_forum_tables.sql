CREATE TABLE board (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(64) UNIQUE NOT NULL,
    description VARCHAR(512),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE post (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL REFERENCES board(id),
    user_id BIGINT NOT NULL REFERENCES "users"(id),
    title VARCHAR(256) NOT NULL,
    content TEXT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'NORMAL' CHECK (status IN ('NORMAL','DELETED')),
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES post(id),
    user_id BIGINT NOT NULL REFERENCES "users"(id),
    parent_id BIGINT REFERENCES comment(id),
    root_id BIGINT REFERENCES comment(id),
    content TEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'NORMAL' CHECK (status IN ('NORMAL','DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_board_created ON post(board_id, created_at);
CREATE INDEX idx_comment_post ON comment(post_id);
CREATE INDEX idx_comment_root ON comment(root_id);

CREATE INDEX idx_comment_flatten ON comment (post_id, COALESCE(root_id, id), created_at) WHERE status = 'NORMAL';

INSERT INTO board (name, slug, description, sort_order) VALUES
    ('综合讨论', 'general', '畅所欲言，分享生活点滴', 1),
    ('技术交流', 'tech', '技术问题讨论与经验分享', 2),
    ('灌水乐园', 'chat', '轻松闲聊，娱乐八卦', 3);