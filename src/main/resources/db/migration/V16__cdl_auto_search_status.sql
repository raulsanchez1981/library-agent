-- Añade columna para rastrear el estado de búsqueda automática en Casa del Libro (NULL=no buscado, AUTO=encontrado, NOT_FOUND=no encontrado)
ALTER TABLE verified_titles
    ADD COLUMN cdl_auto_search_status VARCHAR(20);
