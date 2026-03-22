package com.libraryagent.ingestion.extractor;

/**
 * Origen del enriquecimiento del título en español.
 */
public enum EnrichmentSource {
    /** Sonnet propuso un título y OpenLibrary lo confirmó con un título similar. */
    CONFIRMED,
    /** Sonnet propuso un título pero OpenLibrary devolvió uno diferente. Requiere revisión. */
    REVIEW,
    /** Solo Sonnet tiene el título en español. No verificado por OpenLibrary. */
    SONNET_ONLY,
    /** Sonnet no conocía el título; OpenLibrary lo aportó. */
    OL_ONLY,
    /** Ninguna fuente encontró título en español. */
    NONE
}
