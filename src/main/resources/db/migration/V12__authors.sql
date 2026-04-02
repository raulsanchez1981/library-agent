-- Crea la tabla authors y la relación many-to-many book_authors con extracted_books

CREATE TABLE authors (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(256) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_authors_name_ci ON authors (LOWER(name));

CREATE TABLE book_authors (
    book_id     UUID NOT NULL REFERENCES extracted_books(id) ON DELETE CASCADE,
    author_id   UUID NOT NULL REFERENCES authors(id)         ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);
