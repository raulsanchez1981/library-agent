-- Reemplaza book_scores (esqueleto sin datos) por recommendations con modelo completo

DROP TABLE IF EXISTS book_scores;

CREATE TABLE recommendations (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    extracted_book_id UUID        NOT NULL REFERENCES extracted_books(id) ON DELETE CASCADE,
    score             INTEGER     NOT NULL CHECK (score BETWEEN 0 AND 100),
    reasoning         TEXT        NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'NUEVA' CHECK (status IN ('NUEVA', 'VISTA', 'DESCARTADA')),
    scored_at         TIMESTAMP   NOT NULL DEFAULT now(),
    created_at        TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_recommendations_book UNIQUE (extracted_book_id)
);

CREATE INDEX idx_recommendations_score  ON recommendations(score DESC);
CREATE INDEX idx_recommendations_status ON recommendations(status);
