package com.libraryagent.ingestion.sources;

import com.libraryagent.ingestion.model.RawMention;

import java.util.List;

/**
 * Contrato que toda fuente de ingesta debe implementar.
 * Ver .claude/skills/ingestion/SKILL.md para guía de implementación.
 */
public interface BookSourceIngester {

    /** Identificador único de la fuente (ej: "reddit", "instagram", "rss"). */
    String sourceId();

    /**
     * Extrae menciones de libros desde la fuente.
     * Nunca lanza excepción: ante errores retorna lista vacía y loguea warning.
     */
    List<RawMention> ingest();

    /** Verifica conectividad y credenciales. Timeout máximo 2s. */
    boolean isAvailable();
}
