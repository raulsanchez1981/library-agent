package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.extractor.BookTitleExtractor;
import com.libraryagent.ingestion.extractor.ExtractedBookResult;
import com.libraryagent.ingestion.model.RawMention;
import com.libraryagent.ingestion.sources.BookSourceIngester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock BookSourceIngester ingester;
    @Mock BookTitleExtractor extractor;

    // IngestionService toma List<BookSourceIngester> — @InjectMocks no puede inyectarlo,
    // así que cada test construye la instancia directamente.
    IngestionService service() {
        return new IngestionService(List.of(ingester), extractor);
    }

    @Test
    void shouldDeduplicateMentionsWithSameUrlBeforeExtraction() {
        // Given — dos menciones con la misma URL (duplicado típico de Pullpush)
        RawMention mention = mention("https://reddit.com/r/Fantasy/comments/abc");
        RawMention duplicate = mention("https://reddit.com/r/Fantasy/comments/abc");

        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(mention, duplicate));
        when(extractor.extractBatch(anyList())).thenReturn(List.of());

        // When
        service().runFullIngestion();

        // Then — el extractor solo recibe una mención, el duplicado es descartado
        verify(extractor).extractBatch(List.of(mention));
    }

    @Test
    void shouldDeduplicateAcrossMultipleIngesters() {
        // Given — dos ingesters distintos devuelven la misma URL
        BookSourceIngester ingester2 = mock(BookSourceIngester.class);
        RawMention mention = mention("https://reddit.com/r/Fantasy/comments/xyz");
        RawMention duplicate = mention("https://reddit.com/r/Fantasy/comments/xyz");

        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(mention));
        when(ingester2.isAvailable()).thenReturn(true);
        when(ingester2.ingest()).thenReturn(List.of(duplicate));
        when(extractor.extractBatch(anyList())).thenReturn(List.of());

        IngestionService serviceWithTwo = new IngestionService(List.of(ingester, ingester2), extractor);

        // When
        serviceWithTwo.runFullIngestion();

        // Then
        verify(extractor).extractBatch(List.of(mention));
    }

    @Test
    void shouldPassAllUniqueMentionsToExtractor() {
        // Given — tres menciones con URLs distintas
        RawMention a = mention("https://reddit.com/r/Fantasy/comments/a");
        RawMention b = mention("https://reddit.com/r/Fantasy/comments/b");
        RawMention c = mention("https://reddit.com/r/Fantasy/comments/c");

        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(a, b, c));
        when(extractor.extractBatch(anyList())).thenReturn(List.of());

        // When
        service().runFullIngestion();

        // Then — las tres pasan al extractor
        verify(extractor).extractBatch(List.of(a, b, c));
    }

    @Test
    void shouldReturnEmptyListWhenNoIngesterIsAvailable() {
        // Given
        when(ingester.isAvailable()).thenReturn(false);

        // When
        List<ExtractedBookResult> result = service().runFullIngestion();

        // Then — extractBatch se llama con lista vacía; el resultado es vacío
        assertThat(result).isEmpty();
        verify(extractor).extractBatch(List.of());
    }

    @Test
    void shouldSkipUnavailableIngesterAndProcessAvailableOne() {
        // Given
        BookSourceIngester unavailable = mock(BookSourceIngester.class);
        RawMention mention = mention("https://reddit.com/r/Fantasy/comments/q");

        when(unavailable.isAvailable()).thenReturn(false);
        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(mention));
        when(extractor.extractBatch(anyList())).thenReturn(List.of());

        IngestionService serviceWithTwo = new IngestionService(List.of(unavailable, ingester), extractor);

        // When
        serviceWithTwo.runFullIngestion();

        // Then
        verify(unavailable, never()).ingest();
        verify(extractor).extractBatch(List.of(mention));
    }

    private static RawMention mention(String url) {
        return new RawMention(UUID.randomUUID(), "reddit", "some text", url, Instant.now());
    }
}
