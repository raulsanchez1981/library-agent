-- Añade campos de localización española a extracted_books y constraint de unicidad a raw_mentions

ALTER TABLE extracted_books
    ADD COLUMN title_es              VARCHAR(512),
    ADD COLUMN available_in_spanish  BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE raw_mentions
    ADD CONSTRAINT uq_raw_mentions_url UNIQUE (url);
