-- Añade campos de Google Books (ID, portada y sinopsis) a la tabla verified_titles

ALTER TABLE verified_titles
    ADD COLUMN google_books_id VARCHAR(64),
    ADD COLUMN cover_url       VARCHAR(512),
    ADD COLUMN synopsis        TEXT;
