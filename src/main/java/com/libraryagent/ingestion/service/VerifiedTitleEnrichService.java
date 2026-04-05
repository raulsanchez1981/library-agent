package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.GoogleBooksEnrichmentDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;

import java.util.UUID;

public interface VerifiedTitleEnrichService {

    /** Enriquece desde CDL (manual). Marca como CONFIRMED. */
    VerifiedTitleDetailDto enrichFromCdl(UUID id, String casaDelLibroUrl);

    /** Enriquece desde Google Books automáticamente. Marca como AUTO. */
    VerifiedTitleDetailDto enrichFromGoogleBooks(UUID id, GoogleBooksEnrichmentDto data);

    /** Confirma el libro tal como está (sin cambiar datos). Marca como CONFIRMED. */
    VerifiedTitleDetailDto confirm(UUID id);
}
