-- Añade indicador de saga a los libros extraídos durante la ingesta

ALTER TABLE extracted_books
    ADD COLUMN IF NOT EXISTS is_saga BOOLEAN NOT NULL DEFAULT false;
