-- V15: ampliar verified_titles con campos de Casa del Libro y crear tabla de géneros con relación M:N

ALTER TABLE verified_titles
    ADD COLUMN casa_del_libro_url VARCHAR(1024),
    ADD COLUMN technical_sheet TEXT;

CREATE TABLE genres (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_genres_name UNIQUE (name)
);

CREATE TABLE book_genres (
    verified_title_id UUID NOT NULL REFERENCES verified_titles(id) ON DELETE CASCADE,
    genre_id          UUID NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (verified_title_id, genre_id)
);
