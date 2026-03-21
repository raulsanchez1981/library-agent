package com.libraryagent.ingestion.extractor;

/**
 * Abstracción sobre la llamada a Claude para facilitar el testing sin red.
 */
interface ClaudeGateway {

    /**
     * Envía el texto de una mención y devuelve el JSON bruto que Claude produce.
     * Se espera un array JSON de títulos, ej: ["Dune", "Foundation"].
     */
    String extractTitlesJson(String mentionText);
}
