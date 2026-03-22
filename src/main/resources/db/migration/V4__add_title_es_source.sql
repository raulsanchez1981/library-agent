ALTER TABLE extracted_books
    ADD COLUMN IF NOT EXISTS title_es_source VARCHAR(20);
