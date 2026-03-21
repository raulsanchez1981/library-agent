-- Esquema inicial de LibraryAgent

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE raw_mentions (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    source      VARCHAR(64) NOT NULL,
    text        TEXT        NOT NULL,
    url         VARCHAR(512),
    fetched_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE extracted_books (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title              VARCHAR(512) NOT NULL,
    author             VARCHAR(256),
    source_mention_id  UUID         NOT NULL REFERENCES raw_mentions(id) ON DELETE CASCADE,
    created_at         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE user_profile (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE book_scores (
    id          UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_title  VARCHAR(512)   NOT NULL,
    book_author VARCHAR(256),
    score       DECIMAL(4, 3)  NOT NULL,
    reasons     TEXT,
    scored_at   TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_raw_mentions_source     ON raw_mentions(source);
CREATE INDEX idx_raw_mentions_fetched_at ON raw_mentions(fetched_at);
CREATE INDEX idx_extracted_books_title   ON extracted_books(title);
CREATE INDEX idx_book_scores_score       ON book_scores(score DESC);
