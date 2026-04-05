-- Normaliza nombres de autores a Title Case, añade columna verified a authors,
-- crea tabla verified_titles y vincula libros VERIFIED a sus títulos canonicos.

-- 1. Normalizar nombres de autores existentes a Title Case
UPDATE authors SET name = initcap(name) WHERE name != initcap(name);

-- 2. Añadir columna verified a authors
ALTER TABLE authors ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Marcar como verificados los autores vinculados a libros VERIFIED
UPDATE authors SET verified = TRUE
WHERE id IN (
    SELECT DISTINCT a.id
    FROM authors a
    JOIN book_authors ba ON ba.author_id = a.id
    JOIN extracted_books eb ON eb.id = ba.book_id
    WHERE eb.confidence = 'VERIFIED'
);

-- 3. Crear tabla verified_titles
CREATE TABLE verified_titles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_verified_titles_name_ci ON verified_titles (LOWER(name));

-- 4. Añadir FK verified_title_id a extracted_books
ALTER TABLE extracted_books ADD COLUMN verified_title_id UUID REFERENCES verified_titles(id);

-- 5. Poblar verified_titles desde libros VERIFIED existentes
INSERT INTO verified_titles (name)
SELECT DISTINCT initcap(title_es)
FROM extracted_books
WHERE confidence = 'VERIFIED' AND title_es IS NOT NULL
ON CONFLICT DO NOTHING;

-- 6. Vincular libros VERIFIED a su verified_title
UPDATE extracted_books eb
SET verified_title_id = vt.id
FROM verified_titles vt
WHERE eb.confidence = 'VERIFIED'
  AND eb.title_es IS NOT NULL
  AND LOWER(eb.title_es) = LOWER(vt.name)
  AND eb.verified_title_id IS NULL;

-- 7. Vincular libros no-VERIFIED que ya tengan title_es coincidente
UPDATE extracted_books eb
SET verified_title_id = vt.id
FROM verified_titles vt
WHERE eb.confidence != 'VERIFIED'
  AND eb.verified_title_id IS NULL
  AND eb.title_es IS NOT NULL
  AND LOWER(eb.title_es) = LOWER(vt.name);
