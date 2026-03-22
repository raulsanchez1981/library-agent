package com.libraryagent.ingestion.extractor;

/**
 * Resultado de la extracción rápida con Claude Haiku.
 * No contiene datos de enriquecimiento — esos los añade BookEnrichmentService.
 *
 * @param title  título detectado en el texto
 * @param author autor si se menciona explícitamente, null si no
 * @param isSaga true si es una saga o serie completa
 */
public record ExtractedBookResult(String title, String author, boolean isSaga) {}
