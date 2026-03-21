package com.libraryagent.recommendation.model;

import java.util.List;

/**
 * Preferencias lectoras del usuario usadas por el motor de scoring.
 * Se construye a partir de UserProfile más el historial de scores.
 */
public record UserPreferences(
        List<String> favoriteGenres,
        List<String> favoriteAuthors,
        List<String> avoidedGenres,
        List<String> readBooks,
        int preferredMaxReadingTimeHours
) {
    public static UserPreferences empty() {
        return new UserPreferences(List.of(), List.of(), List.of(), List.of(), 12);
    }
}
