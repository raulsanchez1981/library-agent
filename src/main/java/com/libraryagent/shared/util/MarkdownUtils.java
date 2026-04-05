package com.libraryagent.shared.util;

public final class MarkdownUtils {

    private MarkdownUtils() {}

    /**
     * Elimina las marcas de bloque de código Markdown (triple backtick) de una respuesta de Claude.
     * Maneja variantes como ```json, ```\n...\n``` y texto plano sin fences.
     *
     * @param raw respuesta cruda (puede ser null)
     * @return texto limpio sin fences, o null si la entrada es null
     */
    public static String stripFences(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("```json\\s*", "").replaceAll("```", "").strip();
        }
        return trimmed;
    }
}
