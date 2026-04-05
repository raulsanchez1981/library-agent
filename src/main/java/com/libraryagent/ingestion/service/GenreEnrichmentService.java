package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;

public interface GenreEnrichmentService {

    /** Enriquece géneros de todos los VerifiedTitle sin géneros. Devuelve número de títulos actualizados. */
    int enrichMissingGenres();

    /** Enriquece géneros de TODOS los VerifiedTitle (añade sin duplicar). Async — no devuelve resultado. */
    void enrichAllGenres();

    /** Enriquece géneros de un único VerifiedTitle recién verificado. Async. */
    void enrichSingle(VerifiedTitleEntity verifiedTitle);
}
