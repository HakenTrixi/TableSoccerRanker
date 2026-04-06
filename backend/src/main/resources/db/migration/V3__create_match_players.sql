CREATE TABLE match_players (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id    UUID        NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id),
    team_color  VARCHAR(10) NOT NULL CHECK (team_color IN ('YELLOW', 'WHITE')),
    player_role VARCHAR(10) NOT NULL CHECK (player_role IN ('ATTACKER', 'DEFENDER')),
    UNIQUE (match_id, user_id),
    UNIQUE (match_id, team_color, player_role)
);

CREATE INDEX idx_match_players_user ON match_players(user_id);
CREATE INDEX idx_match_players_match ON match_players(match_id);
