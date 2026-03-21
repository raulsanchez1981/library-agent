package com.libraryagent.ingestion.extractor;

/**
 * Resultado de extraer un libro de una RawMention, enriquecido con datos de OpenLibrary.
 *
 * @param title             título original detectado por Claude
 * @param author            autor (null si OpenLibrary no lo encontró)
 * @param titleEs           título en español (null si no difiere o no se encontró)
 * @param availableInSpanish true si OpenLibrary tiene al menos una edición en español
 */
public record ExtractedBookResult(
        String title,
        String author,
        String titleEs,
        boolean availableInSpanish
) {}
