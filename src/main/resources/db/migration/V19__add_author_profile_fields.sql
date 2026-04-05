-- Añade campos de perfil de autor: foto, biografía e identificador de Open Library

ALTER TABLE authors
    ADD COLUMN IF NOT EXISTS photo_url TEXT,
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS open_library_olid VARCHAR(64);

COMMENT ON COLUMN authors.photo_url IS 'URL de la foto del autor';
COMMENT ON COLUMN authors.bio IS 'Biografía corta del autor';
COMMENT ON COLUMN authors.open_library_olid IS 'Identificador del autor en Open Library (ej: OL23919A)';
