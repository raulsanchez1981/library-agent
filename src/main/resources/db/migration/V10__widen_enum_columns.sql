-- Amplía las columnas confidence y enrichment_source para soportar valores más largos (VERIFIED=8, ADMIN=5)

ALTER TABLE extracted_books
    ALTER COLUMN confidence TYPE VARCHAR(10),
    ALTER COLUMN enrichment_source TYPE VARCHAR(20);
