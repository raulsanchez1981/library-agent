-- Campos de enriquecimiento asíncrono de libros extraídos
ALTER TABLE extracted_books
    ADD COLUMN IF NOT EXISTS enriched          BOOLEAN   NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS enriched_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS enrichment_source VARCHAR(20),
    ADD COLUMN IF NOT EXISTS title_es_ol       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS author_corrected  VARCHAR(300);

-- title_es_source queda obsoleto, sustituido por enrichment_source
ALTER TABLE extracted_books DROP COLUMN IF EXISTS title_es_source;
