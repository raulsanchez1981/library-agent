package com.libraryagent.ingestion.extractor;

import java.util.Optional;

public interface OpenLibraryClient {

    /**
     * Busca si existe edición en español para el título dado.
     * Devuelve empty solo si hay un error de red o inesperado.
     */
    Optional<SpanishEdition> findSpanishEdition(String englishTitle);
}
