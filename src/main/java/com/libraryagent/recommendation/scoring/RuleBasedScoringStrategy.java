package com.libraryagent.recommendation.scoring;

import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.recommendation.model.ScoringResult;
import com.libraryagent.recommendation.model.UserPreferences;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(BookScoringStrategy.class)
public final class RuleBasedScoringStrategy implements BookScoringStrategy {

    private static final int BASE_SCORE = 50;
    private static final int AUTHOR_BONUS = 20;
    private static final int GENRE_BONUS = 15;

    @Override
    public ScoringResult score(ExtractedBookEntity book, UserPreferences preferences) {
        String title = resolveTitle(book);
        String author = resolveAuthor(book);

        // Penalización total si el libro ya está leído
        boolean alreadyRead = preferences.completedBookTitles().stream()
                .anyMatch(read -> read.equalsIgnoreCase(title));
        if (alreadyRead) {
            return new ScoringResult(0, "Puntuación basada en reglas (sin API Claude disponible)");
        }

        int rawScore = BASE_SCORE;

        // Bonificación por autor favorito
        boolean isFavoriteAuthor = preferences.favoriteAuthors().stream()
                .anyMatch(fav -> fav.equalsIgnoreCase(author));
        if (isFavoriteAuthor) {
            rawScore += AUTHOR_BONUS;
        }

        // Bonificación por géneros favoritos que aparezcan en el título (heurística)
        String titleLower = title.toLowerCase();
        long genreMatches = preferences.favoriteGenres().stream()
                .filter(genre -> titleLower.contains(genre.toLowerCase()))
                .count();
        rawScore += (int) (genreMatches * GENRE_BONUS);

        int clampedScore = Math.max(0, Math.min(100, rawScore));
        return new ScoringResult(clampedScore, "Puntuación basada en reglas (sin API Claude disponible)");
    }

    private String resolveTitle(ExtractedBookEntity book) {
        if (book.getTitleEs() != null && !book.getTitleEs().isBlank()) {
            return book.getTitleEs();
        }
        return book.getTitle();
    }

    private String resolveAuthor(ExtractedBookEntity book) {
        if (book.getAuthorCorrected() != null && !book.getAuthorCorrected().isBlank()) {
            return book.getAuthorCorrected();
        }
        return book.getAuthor() != null ? book.getAuthor() : "";
    }
}
