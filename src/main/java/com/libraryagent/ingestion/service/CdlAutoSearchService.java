package com.libraryagent.ingestion.service;

import java.util.List;
import java.util.UUID;

public interface CdlAutoSearchService {

    /**
     * Para cada verifiedTitleId: busca en Google la ficha de Casa del Libro.
     * Si la encuentra, enriquece el título (estado AUTO).
     * Si no, marca NOT_FOUND.
     * Se ejecuta en background — no bloquea al caller.
     */
    void searchAndEnrich(List<UUID> verifiedTitleIds);

    /**
     * Igual que searchAndEnrich pero busca automáticamente todos los títulos
     * pendientes (sin cdlAutoSearchStatus y sin casaDelLibroUrl asignada).
     */
    void searchAndEnrichAll();

    /**
     * Fuerza re-enriquecimiento de todos los títulos con estado AUTO (sin CDL),
     * independientemente de si ya tienen datos. Útil para corregir enriquecimientos
     * anteriores con datos incompletos o portadas incorrectas.
     */
    void reEnrichAllGoogleBooks();
}
