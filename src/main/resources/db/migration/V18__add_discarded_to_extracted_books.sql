-- Añade columna discarded a extracted_books para ocultar libros sin eliminarlos

ALTER TABLE extracted_books
    ADD COLUMN IF NOT EXISTS discarded BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN extracted_books.discarded IS 'Indica que el usuario ha descartado este libro; no aparece en el listado de ingestados pero no se elimina';
