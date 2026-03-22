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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookTitleExtractorTest {

    @Mock ClaudeGateway claudeGateway;

    BookTitleExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new BookTitleExtractor(claudeGateway, new ObjectMapper());
    }

    @Test
    void shouldExtractTitleAuthorAndIsSagaFromHaikuResponse() {
        // Given
        when(claudeGateway.extractBooksJson(anyString())).thenReturn("""
                [
                  {"title":"Dune","author":"Frank Herbert","isSaga":false},
                  {"title":"Wheel of Time","author":null,"isSaga":true}
                ]""");

        // When
        List<ExtractedBookResult> result = extractor.extract(mention("I read Dune and Wheel of Time"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Dune");
        assertThat(result.get(0).author()).isEqualTo("Frank Herbert");
        assertThat(result.get(0).isSaga()).isFalse();
        assertThat(result.get(1).title()).isEqualTo("Wheel of Time");
        assertThat(result.get(1).author()).isNull();
        assertThat(result.get(1).isSaga()).isTrue();
    }

    @Test
    void shouldNotCallSonnetNorOpenLibrary() {
        // Given
        when(claudeGateway.extractBooksJson(anyString())).thenReturn("[{\"title\":\"Dune\",\"author\":null,\"isSaga\":false}]");

        // When
        extractor.extract(mention("Dune is great"));

        // Then — una sola llamada a Haiku, ninguna a Sonnet ni OL
        verify(claudeGateway).extractBooksJson(anyString());
        verify(claudeGateway, never()).enrichBooksBatchJson(anyString());
    }

    @Test
    void shouldReturnEmptyWhenHaikuReturnsEmptyArray() {
        // Given
        when(claudeGateway.extractBooksJson(anyString())).thenReturn("[]");

        // When
        List<ExtractedBookResult> result = extractor.extract(mention("Nothing book-related."));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenHaikuReturnsInvalidJson() {
        // Given
        when(claudeGateway.extractBooksJson(anyString())).thenReturn("esto no es json {{{{");

        // When
        List<ExtractedBookResult> result = extractor.extract(mention("Some text."));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSkipEntriesWithNullOrBlankTitle() {
        // Given
        when(claudeGateway.extractBooksJson(anyString())).thenReturn("""
                [
                  {"title":null,"author":null,"isSaga":false},
                  {"title":"","author":null,"isSaga":false},
                  {"title":"Foundation","author":"Asimov","isSaga":false}
                ]""");

        // When
        List<ExtractedBookResult> result = extractor.extract(mention("Foundation and others"));

        // Then — solo Foundation, los dos anteriores descartados
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Foundation");
    }

    @Test
    void shouldParseMarkdownFencesFromHaiku() {
        // Given
        when(claudeGateway.extractBooksJson(anyString()))
                .thenReturn("```json\n[{\"title\":\"Neuromancer\",\"author\":\"William Gibson\",\"isSaga\":false}]\n```");

        // When
        List<ExtractedBookResult> result = extractor.extract(mention("Read Neuromancer."));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Neuromancer");
        assertThat(result.get(0).author()).isEqualTo("William Gibson");
    }

    private RawMention mention(String text) {
        return new RawMention(UUID.randomUUID(), "reddit/Fantasy", text,
                "https://reddit.com/r/Fantasy/test", Instant.now());
    }
}
