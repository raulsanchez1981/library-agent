package com.libraryagent.ingestion.model;

import java.util.UUID;

/**
 * Libro extraído a partir de una RawMention mediante análisis de Claude API.
 */
public record ExtractedBook(
        UUID id,
        String title,
        String author,
        UUID sourceMentionId
) {}
