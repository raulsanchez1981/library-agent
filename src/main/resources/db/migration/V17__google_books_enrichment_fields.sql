-- Añade campos de enriquecimiento de Google Books y migra registros verificados manualmente

ALTER TABLE verified_titles ADD COLUMN publisher VARCHAR(256);
ALTER TABLE verified_titles ADD COLUMN published_date VARCHAR(32);
ALTER TABLE verified_titles ADD COLUMN page_count INTEGER;
ALTER TABLE verified_titles ADD COLUMN isbn VARCHAR(20);

-- Libros con URL de Casa del Libro pero sin estado CDL fueron verificados manualmente antes
-- de introducir el enum; se migran al estado CONFIRMED
UPDATE verified_titles
SET cdl_auto_search_status = 'CONFIRMED'
WHERE cdl_auto_search_status IS NULL
  AND casa_del_libro_url IS NOT NULL;
