package com.libraryagent.ingestion.extractor;

/**
 * Origen del enriquecimiento del título en español.
 */
public enum EnrichmentSource {
    /** Sonnet devolvió titleEs (con o sin confirmación de OpenLibrary). */
    SONNET,
    /** Sonnet devolvió null pero OpenLibrary encontró una edición en español. */
    OL_ONLY,
    /** Ninguna fuente encontró título en español. */
    NONE,
    /** El administrador corrigió manualmente el libro. */
    ADMIN
}
