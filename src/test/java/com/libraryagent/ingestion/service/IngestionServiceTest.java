package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.extractor.BookTitleExtractor;
import com.libraryagent.ingestion.extractor.ExtractedBookResult;
import com.libraryagent.ingestion.model.RawMention;
import com.libraryagent.ingestion.model.RawMentionEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.RawMentionRepository;
import com.libraryagent.ingestion.sources.BookSourceIngester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock BookSourceIngester ingester;
    @Mock BookTitleExtractor extractor;
    @Mock RawMentionRepository rawMentionRepository;
    @Mock ExtractedBookRepository extractedBookRepository;

    IngestionService service() {
        return new IngestionService(List.of(ingester), extractor, rawMentionRepository, extractedBookRepository);
    }

    @Test
    void shouldDeduplicateMentionsWithSameUrlBeforeExtraction() {
        // Given — dos menciones con la misma URL (duplicado típico de Pullpush)
        RawMention mention = mention("https://reddit.com/r/Fantasy/comments/abc");
        RawMention duplicate = mention("https://reddit.com/r/Fantasy/comments/abc");

        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(mention, duplicate));
        when(rawMentionRepository.existsByUrl(anyString())).thenReturn(false);
        when(rawMentionRepository.save(any())).thenReturn(new RawMentionEntity());
        when(extractor.extract(any())).thenReturn(List.of());

        // When
        service().runFullIngestion();

        // Then — el extractor recibe solo una mención, el duplicado es descartado
        verify(extractor, times(1)).extract(mention);
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
        when(rawMentionRepository.existsByUrl(anyString())).thenReturn(false);
        when(rawMentionRepository.save(any())).thenReturn(new RawMentionEntity());
        when(extractor.extract(any())).thenReturn(List.of());

        IngestionService serviceWithTwo =
                new IngestionService(List.of(ingester, ingester2), extractor, rawMentionRepository, extractedBookRepository);

        // When
        serviceWithTwo.runFullIngestion();

        // Then — solo se procesa la primera mención
        verify(extractor, times(1)).extract(mention);
    }

    @Test
    void shouldSkipMentionsAlreadyPersistedInDatabase() {
        // Given — mención que ya existe en BBDD
        RawMention mention = mention("https://reddit.com/r/Fantasy/comments/known");

        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(mention));
        when(rawMentionRepository.existsByUrl(mention.url())).thenReturn(true);

        // When
        List<ExtractedBookResult> result = service().runFullIngestion();

        // Then — nada se guarda ni se extrae
        assertThat(result).isEmpty();
        verify(rawMentionRepository, never()).save(any());
        verify(extractor, never()).extract(any());
    }

    @Test
    void shouldPassAllUniqueMentionsToExtractor() {
        // Given — tres menciones con URLs distintas
        RawMention a = mention("https://reddit.com/r/Fantasy/comments/a");
        RawMention b = mention("https://reddit.com/r/Fantasy/comments/b");
        RawMention c = mention("https://reddit.com/r/Fantasy/comments/c");

        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(a, b, c));
        when(rawMentionRepository.existsByUrl(anyString())).thenReturn(false);
        when(rawMentionRepository.save(any())).thenReturn(new RawMentionEntity());
        when(extractor.extract(any())).thenReturn(List.of());

        // When
        service().runFullIngestion();

        // Then — las tres pasan al extractor
        verify(extractor).extract(a);
        verify(extractor).extract(b);
        verify(extractor).extract(c);
    }

    @Test
    void shouldReturnEmptyListWhenNoIngesterIsAvailable() {
        // Given
        when(ingester.isAvailable()).thenReturn(false);

        // When
        List<ExtractedBookResult> result = service().runFullIngestion();

        // Then
        assertThat(result).isEmpty();
        verify(extractor, never()).extract(any());
        verify(rawMentionRepository, never()).save(any());
    }

    @Test
    void shouldSkipUnavailableIngesterAndProcessAvailableOne() {
        // Given
        BookSourceIngester unavailable = mock(BookSourceIngester.class);
        RawMention mention = mention("https://reddit.com/r/Fantasy/comments/q");

        when(unavailable.isAvailable()).thenReturn(false);
        when(ingester.isAvailable()).thenReturn(true);
        when(ingester.ingest()).thenReturn(List.of(mention));
        when(rawMentionRepository.existsByUrl(anyString())).thenReturn(false);
        when(rawMentionRepository.save(any())).thenReturn(new RawMentionEntity());
        when(extractor.extract(any())).thenReturn(List.of());

        IngestionService serviceWithTwo =
                new IngestionService(List.of(unavailable, ingester), extractor, rawMentionRepository, extractedBookRepository);

        // When
        serviceWithTwo.runFullIngestion();

        // Then
        verify(unavailable, never()).ingest();
        verify(extractor).extract(mention);
    }

    private static RawMention mention(String url) {
        return new RawMention(UUID.randomUUID(), "reddit", "some text", url, Instant.now());
    }
}
