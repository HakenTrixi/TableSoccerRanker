CREATE TABLE matches (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    yellow_score   INTEGER     NOT NULL CHECK (yellow_score >= 0),
    white_score    INTEGER     NOT NULL CHECK (white_score >= 0),
    played_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    recorded_by_id UUID        NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_matches_played_at ON matches(played_at DESC);
