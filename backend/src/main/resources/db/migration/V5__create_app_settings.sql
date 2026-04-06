CREATE TABLE app_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      VARCHAR(500) NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO app_settings (key, value) VALUES ('long_term_algorithm', 'ELO');
INSERT INTO app_settings (key, value) VALUES ('monthly_algorithm', 'MONTHLY_ELO_GAIN');
