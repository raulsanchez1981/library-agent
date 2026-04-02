-- Elimina duplicados en extracted_books (mismo título, case-insensitive) y añade índice único

-- 1. Eliminar recommendations de los duplicados que se van a borrar
DELETE FROM recommendations
WHERE extracted_book_id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY LOWER(title)
                   ORDER BY enriched DESC, created_at ASC
               ) AS rn
        FROM extracted_books
    ) ranked
    WHERE rn > 1
);

-- 2. Eliminar los extracted_books duplicados
DELETE FROM extracted_books
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY LOWER(title)
                   ORDER BY enriched DESC, created_at ASC
               ) AS rn
        FROM extracted_books
    ) ranked
    WHERE rn > 1
);

-- 3. Índice único case-insensitive sobre el título original
CREATE UNIQUE INDEX uq_extracted_books_title_ci ON extracted_books (LOWER(title));
