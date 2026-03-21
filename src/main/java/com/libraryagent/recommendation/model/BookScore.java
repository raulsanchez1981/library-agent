package com.libraryagent.recommendation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado del scoring de un libro contra el perfil del usuario.
 * Ver .claude/skills/recommendation/SKILL.md para el formato completo.
 */
public record BookScore(
        UUID id,
        String bookTitle,
        String bookAuthor,
        double score,
        List<String> reasons,
        Instant scoredAt
) {
    public boolean meetsThreshold(double threshold) {
        return score >= threshold;
    }
}
