-- Añade columna confidence y consolida los valores de enrichment_source en tres estados (SONNET, OL_ONLY, NONE)

ALTER TABLE extracted_books
    ADD COLUMN confidence VARCHAR(6);

UPDATE extracted_books SET enrichment_source = 'SONNET', confidence = 'HIGH'    WHERE enrichment_source = 'CONFIRMED';
UPDATE extracted_books SET enrichment_source = 'SONNET', confidence = 'LOW'     WHERE enrichment_source = 'REVIEW';
UPDATE extracted_books SET enrichment_source = 'SONNET', confidence = 'MEDIUM'  WHERE enrichment_source = 'SONNET_ONLY';
