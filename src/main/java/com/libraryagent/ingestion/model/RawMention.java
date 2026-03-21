package com.libraryagent.ingestion.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Mención en bruto recogida de una fuente externa, antes de extracción de libro.
 */
public record RawMention(
        UUID id,
        String source,
        String text,
        String url,
        Instant fetchedAt
) {}
