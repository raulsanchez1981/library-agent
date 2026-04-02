package com.libraryagent.ingestion.extractor;

import java.util.Optional;

public interface OpenLibraryClient {

    /**
     * Busca una edición en español partiendo del título en inglés.
     * Usado cuando Sonnet no conoce la traducción (fallback OL_ONLY).
     */
    Optional<SpanishEdition> findSpanishEdition(String englishTitle);

    /**
     * Verifica si OpenLibrary reconoce un título en español dado.
     * Usado para confirmar o contrastar la traducción propuesta por Sonnet.
     * Devuelve el título en español que OL tiene catalogado (puede diferir del buscado).
     */
    Optional<SpanishEdition> findBySpanishTitle(String spanishTitle);

    /**
     * Busca la URL de portada en alta resolución para un título dado.
     * Intenta primero con el título en español, luego con el original.
     */
    Optional<String> findCoverUrl(String title, String originalTitle);
}
