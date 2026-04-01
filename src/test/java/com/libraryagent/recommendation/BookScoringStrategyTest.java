package com.libraryagent.recommendation;

import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.recommendation.model.ScoringResult;
import com.libraryagent.recommendation.model.UserPreferences;
import com.libraryagent.recommendation.scoring.RuleBasedScoringStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookScoringStrategyTest {

    private RuleBasedScoringStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RuleBasedScoringStrategy();
    }

    @Test
    void shouldReturnZeroScoreWhenBookAlreadyRead() {
        // Given
        ExtractedBookEntity book = bookWithTitle("Dune");
        UserPreferences preferences = prefsWithCompletedBooks(List.of("Dune"));

        // When
        ScoringResult result = strategy.score(book, preferences);

        // Then
        assertThat(result.score()).isEqualTo(0);
    }

    @Test
    void shouldBonusScoreWhenAuthorIsFavorite() {
        // Given
        ExtractedBookEntity book = bookWithAuthor("Frank Herbert");
        UserPreferences preferences = prefsWithFavoriteAuthors(List.of("Frank Herbert"));

        // When
        ScoringResult result = strategy.score(book, preferences);

        // Then
        // Base 50 + bonus autor 20 = 70
        assertThat(result.score()).isEqualTo(70);
    }

    @Test
    void shouldReturnBaseScoreWhenNoMatchFound() {
        // Given
        ExtractedBookEntity book = bookWithTitle("Un libro desconocido");
        UserPreferences preferences = new UserPreferences(
                List.of("ciencia ficción"),
                List.of("Isaac Asimov"),
                "es",
                75,
                List.of()
        );

        // When
        ScoringResult result = strategy.score(book, preferences);

        // Then
        assertThat(result.score()).isEqualTo(50);
    }

    @Test
    void shouldClampScoreBetweenZeroAndHundred() {
        // Given — varios géneros favoritos para superar 100
        ExtractedBookEntity book = bookWithTitle("fantasía magia dragones misterio aventura thriller");
        UserPreferences preferences = new UserPreferences(
                List.of("fantasía", "magia", "dragones", "misterio", "aventura", "thriller"),
                List.of("autor favorito"),
                "es",
                75,
                List.of()
        );
        book.setAuthorCorrected("autor favorito");

        // When
        ScoringResult result = strategy.score(book, preferences);

        // Then
        assertThat(result.score()).isLessThanOrEqualTo(100);
        assertThat(result.score()).isGreaterThanOrEqualTo(0);
    }

    // --- helpers ---

    private ExtractedBookEntity bookWithTitle(String title) {
        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setTitle(title);
        return book;
    }

    private ExtractedBookEntity bookWithAuthor(String author) {
        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setTitle("Dune");
        book.setAuthor(author);
        return book;
    }

    private UserPreferences prefsWithCompletedBooks(List<String> completed) {
        return new UserPreferences(List.of(), List.of(), "es", 75, completed);
    }

    private UserPreferences prefsWithFavoriteAuthors(List<String> authors) {
        return new UserPreferences(List.of(), authors, "es", 75, List.of());
    }
}
