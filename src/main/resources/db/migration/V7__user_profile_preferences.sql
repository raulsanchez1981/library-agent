-- Añade campos de preferencias lectoras a user_profile y crea tablas de join para géneros y autores favoritos

ALTER TABLE user_profile
    ADD COLUMN preferred_language   VARCHAR(10)   NOT NULL DEFAULT 'es',
    ADD COLUMN min_score_threshold  DECIMAL(3,2)  NOT NULL DEFAULT 0.75;

CREATE TABLE user_profile_favorite_genres (
    profile_id  UUID         NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    genre       VARCHAR(100) NOT NULL
);

CREATE TABLE user_profile_favorite_authors (
    profile_id  UUID         NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    author      VARCHAR(256) NOT NULL
);
