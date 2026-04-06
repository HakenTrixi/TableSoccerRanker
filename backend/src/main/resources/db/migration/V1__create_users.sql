CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    avatar_url   VARCHAR(500),
    google_sub   VARCHAR(255) NOT NULL UNIQUE,
    role         VARCHAR(20)  NOT NULL DEFAULT 'PLAYER',
    elo_rating   INTEGER      NOT NULL DEFAULT 1200,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
