package com.libraryagent.ingestion.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.model.RawMention;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookTitleExtractorTest {

    @Mock
    ClaudeGateway claudeGateway;

    @Mock
    OpenLibraryClient openLibraryClient;

    BookTitleExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new BookTitleExtractor(claudeGateway, openLibraryClient, new ObjectMapper());
    }

    @Test
    void shouldReturnEnrichedBooksWhenClaudeExtractsTitles() {
        // Given
        RawMention mention = mention("I just finished Dune and Foundation, both amazing!");
        when(claudeGateway.extractTitlesJson(anyString())).thenReturn("[\"Dune\",\"Foundation\"]");
        when(openLibraryClient.findSpanishEdition("Dune"))
                .thenReturn(Optional.of(new SpanishEdition("Dune", "Dune", "Frank Herbert", true)));
        when(openLibraryClient.findSpanishEdition("Foundation"))
                .thenReturn(Optional.of(new SpanishEdition("Fundación", "Foundation", "Isaac Asimov", true)));

        // When
        List<ExtractedBookResult> result = extractor.extract(mention);

        // Then
        assertThat(result).hasSize(2);

        ExtractedBookResult dune = result.get(0);
        assertThat(dune.title()).isEqualTo("Dune");
        assertThat(dune.author()).isEqualTo("Frank Herbert");
        assertThat(dune.availableInSpanish()).isTrue();

        ExtractedBookResult foundation = result.get(1);
        assertThat(foundation.title()).isEqualTo("Foundation");
        assertThat(foundation.author()).isEqualTo("Isaac Asimov");
        assertThat(foundation.titleEs()).isEqualTo("Fundación");
        assertThat(foundation.availableInSpanish()).isTrue();
    }

    @Test
    void shouldReturnEmptyListWhenClaudeReturnsEmptyArray() {
        // Given
        RawMention mention = mention("Nothing book-related here.");
        when(claudeGateway.extractTitlesJson(anyString())).thenReturn("[]");

        // When
        List<ExtractedBookResult> result = extractor.extract(mention);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenClaudeReturnsInvalidJson() {
        // Given
        RawMention mention = mention("Some mention text.");
        when(claudeGateway.extractTitlesJson(anyString())).thenReturn("esto no es json valido {{{{");

        // When — no debe lanzar excepción
        List<ExtractedBookResult> result = extractor.extract(mention);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseCorrectlyWhenClaudeWrapsJsonInMarkdownFences() {
        // Given
        RawMention mention = mention("I recommend Dune.");
        when(claudeGateway.extractTitlesJson(anyString())).thenReturn("```json\n[\"Dune\"]\n```");
        when(openLibraryClient.findSpanishEdition("Dune"))
                .thenReturn(Optional.of(new SpanishEdition(null, "Dune", "Frank Herbert", true)));

        // When
        List<ExtractedBookResult> result = extractor.extract(mention);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Dune");
        assertThat(result.get(0).author()).isEqualTo("Frank Herbert");
    }

    private RawMention mention(String text) {
        return new RawMention(UUID.randomUUID(), "reddit", text, "https://reddit.com/r/test", Instant.now());
    }
}
