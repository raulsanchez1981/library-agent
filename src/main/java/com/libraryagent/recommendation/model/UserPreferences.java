package com.libraryagent.recommendation.model;

import java.util.List;

/**
 * Preferencias lectoras del usuario usadas por el motor de scoring.
 * Se construye a partir de UserProfile más el historial de lectura con status READ.
 */
public record UserPreferences(
        List<String> favoriteGenres,
        List<String> favoriteAuthors,
        String preferredLanguage,
        int minScore,
        List<String> completedBookTitles
) {}
