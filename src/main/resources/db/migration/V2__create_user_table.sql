create table IF NOT EXISTS "users" (
    id bigserial primary key,
    username varchar(32) unique not null,
    email varchar(128) unique not null,
    password_hash varchar(255) not null,
    nickname varchar(64),
    avatar_path varchar(255),
    bio varchar(512),
    role varchar(16) not null default 'USER' check (role in ('USER', 'MODERATOR', 'ADMIN')),
    status varchar(16) not null default 'ACTIVE' check (status IN ('ACTIVE','BANNED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);