package com.libraryagent.ingestion.service;

public interface GenreEnrichmentService {

    /** Enriquece géneros de todos los VerifiedTitle sin géneros. Devuelve número de títulos actualizados. */
    int enrichMissingGenres();

    /** Enriquece géneros de TODOS los VerifiedTitle (añade sin duplicar). Async — no devuelve resultado. */
    void enrichAllGenres();
}
