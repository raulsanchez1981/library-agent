package com.libraryagent.ingestion.extractor;

/**
 * Resultado de la búsqueda de edición en español en OpenLibrary.
 *
 * @param titleEs       título en español (null si OpenLibrary no lo tiene)
 * @param titleOriginal título en inglés con el que se realizó la búsqueda
 * @param author        autor del libro (null si no encontrado)
 * @param available     true si existe al menos una edición en español en OpenLibrary
 */
public record SpanishEdition(
        String titleEs,
        String titleOriginal,
        String author,
        boolean available
) {}
