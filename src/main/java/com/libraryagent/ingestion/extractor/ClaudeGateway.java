package com.libraryagent.ingestion.extractor;

/**
 * Abstracción sobre las llamadas a Claude para facilitar el testing sin red.
 */
public interface ClaudeGateway {

    /**
     * Usa Claude Haiku para extraer los libros mencionados en el texto.
     * Devuelve un array JSON de objetos: [{"title":"...","author":"...","isSaga":false}, ...]
     */
    String extractBooksJson(String mentionText);

    /**
     * Usa Claude Sonnet para enriquecer un batch de libros con título en español y autor corregido.
     * Entrada:  array JSON [{"title":"...","author":"...","isSaga":false}, ...]
     * Salida:   array JSON en el mismo orden: [{"titleEs":"...","authorCorrected":"...","isSaga":false}, ...]
     */
    String enrichBooksBatchJson(String booksJson);

    /**
     * Usa Claude Sonnet para buscar el autor de libros donde author es desconocido.
     * Entrada:  array JSON [{"title":"...","isSaga":false}, ...]
     * Salida:   array JSON en el mismo orden: [{"author":"..."}, ...]
     *           author puede ser null si Sonnet no conoce el libro.
     */
    String lookupAuthorsBatchJson(String booksJson);

    /**
     * Usa Claude Haiku para inferir géneros literarios de una lista de libros.
     * Entrada: array JSON [{"title":"...","author":"...","synopsis":"..."}, ...]
     *   synopsis puede ser null — en ese caso usa solo título y autor
     * Salida: array JSON en el mismo orden: [{"genres":["Fantasía épica","Aventura"]}, ...]
     */
    String inferGenresBatchJson(String booksJson);
}
