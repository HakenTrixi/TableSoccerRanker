CREATE TABLE elo_snapshots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID    NOT NULL REFERENCES users(id),
    elo_rating    INTEGER NOT NULL,
    snapshot_date DATE    NOT NULL,
    UNIQUE (user_id, snapshot_date)
);

CREATE INDEX idx_elo_snapshots_user_date ON elo_snapshots(user_id, snapshot_date DESC);
