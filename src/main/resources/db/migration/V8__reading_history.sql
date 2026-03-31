-- Crea la tabla reading_history con status como VARCHAR(20) para compatibilidad con JPA @Enumerated(STRING)

CREATE TABLE reading_history (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    profile_id  UUID          NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    book_title  VARCHAR(512)  NOT NULL,
    book_author VARCHAR(256),
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    started_at  DATE,
    finished_at DATE,
    rating      SMALLINT      CHECK (rating BETWEEN 1 AND 5),
    notes       TEXT,
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_reading_history_profile_id ON reading_history(profile_id);
CREATE INDEX idx_reading_history_status     ON reading_history(status);
