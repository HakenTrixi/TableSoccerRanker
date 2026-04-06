ALTER TABLE match_players ADD COLUMN elo_before      INTEGER;
ALTER TABLE match_players ADD COLUMN elo_after       INTEGER;
ALTER TABLE match_players ADD COLUMN elo_change      INTEGER;
ALTER TABLE match_players ADD COLUMN team_elo        DOUBLE PRECISION;
ALTER TABLE match_players ADD COLUMN win_probability DOUBLE PRECISION;
