package com.libraryagent.ingestion.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorNameParserTest {

    @Test
    void shouldReturnEmptyListWhenInputIsNull() {
        assertThat(AuthorNameParser.parse(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenInputIsBlank() {
        assertThat(AuthorNameParser.parse("   ")).isEmpty();
    }

    @Test
    void shouldReturnSingleAuthorWhenNoSeparators() {
        assertThat(AuthorNameParser.parse("Frank Herbert")).containsExactly("Frank Herbert");
    }

    @Test
    void shouldSplitByAmpersand() {
        List<String> result = AuthorNameParser.parse("Stephen King & Peter Straub");
        assertThat(result).containsExactly("Stephen King", "Peter Straub");
    }

    @Test
    void shouldSplitByCommaSpace() {
        List<String> result = AuthorNameParser.parse("García Márquez, G.");
        assertThat(result).containsExactly("García Márquez", "G.");
    }

    @Test
    void shouldSplitByAndKeyword() {
        List<String> result = AuthorNameParser.parse("Terry Pratchett and Neil Gaiman");
        assertThat(result).containsExactly("Terry Pratchett", "Neil Gaiman");
    }

    @Test
    void shouldHandleCombinedSeparators() {
        // "A, B & C" → split by ", " → ["A", "B & C"] → split by " & " → ["A", "B", "C"]
        List<String> result = AuthorNameParser.parse("Author A, Author B & Author C");
        assertThat(result).containsExactly("Author A", "Author B", "Author C");
    }

    @Test
    void shouldDeduplicateAuthors() {
        List<String> result = AuthorNameParser.parse("Frank Herbert & Frank Herbert");
        assertThat(result).containsExactly("Frank Herbert");
    }

    @Test
    void shouldStripWhitespaceAroundNames() {
        List<String> result = AuthorNameParser.parse("  Terry Pratchett  &  Neil Gaiman  ");
        assertThat(result).containsExactly("Terry Pratchett", "Neil Gaiman");
    }
}
