package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookEnrichmentServiceImpl implements BookEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(BookEnrichmentServiceImpl.class);
    private static final int BATCH_SIZE = 10;

    private final ExtractedBookRepository repository;
    private final ClaudeResponseParser claudeResponseParser;
    private final SpanishTitleResolver spanishTitleResolver;
    private final AuthorEnricher authorEnricher;
    private final BookLinkingService bookLinkingService;
    private final ObjectMapper objectMapper;

    public BookEnrichmentServiceImpl(
            ExtractedBookRepository repository,
            ClaudeResponseParser claudeResponseParser,
            SpanishTitleResolver spanishTitleResolver,
            AuthorEnricher authorEnricher,
            BookLinkingService bookLinkingService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.claudeResponseParser = claudeResponseParser;
        this.spanishTitleResolver = spanishTitleResolver;
        this.authorEnricher = authorEnricher;
        this.bookLinkingService = bookLinkingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public long countPendingEnrichment() {
        return repository.countByEnrichedFalse();
    }

    @Override
    public void enrichPending() {
        List<ExtractedBookEntity> pending = repository.findByEnrichedFalse();
        if (pending.isEmpty()) {
            log.info("No hay libros pendientes de enriquecimiento");
            return;
        }
        log.info("Enriqueciendo {} libros pendientes", pending.size());

        for (int i = 0; i < pending.size(); i += BATCH_SIZE) {
            List<ExtractedBookEntity> batch = pending.subList(i, Math.min(i + BATCH_SIZE, pending.size()));
            enrichBatch(batch);
        }

        bookLinkingService.linkUnverifiedBooks();
        log.info("Enriquecimiento completado");
    }

    @Override
    public int reEnrichAuthors() {
        List<ExtractedBookEntity> candidates = repository.findByEnrichedTrueAndAuthorIsNull();
        if (candidates.isEmpty()) {
            log.info("No hay libros enriquecidos sin autor");
            return 0;
        }
        int recovered = 0;

        for (int i = 0; i < candidates.size(); i += BATCH_SIZE) {
            List<ExtractedBookEntity> batch = candidates.subList(i, Math.min(i + BATCH_SIZE, candidates.size()));
            String booksJson = serializeBatchForLookup(batch);
            List<String> authors = claudeResponseParser.parseAuthorLookup(booksJson);

            List<ExtractedBookEntity> toSave = new ArrayList<>();
            for (int j = 0; j < batch.size(); j++) {
                ExtractedBookEntity entity = batch.get(j);
                String author = j < authors.size() ? authors.get(j) : null;
                if (author != null) {
                    entity.setAuthor(author);
                    entity.setAuthorCorrected(author);
                    List<AuthorEntity> authorEntities = AuthorNameParser.parse(author).stream()
                            .map(authorEnricher::findOrCreate)
                            .toList();
                    entity.getAuthors().addAll(authorEntities);
                    toSave.add(entity);
                    recovered++;
                }
            }

            if (!toSave.isEmpty()) {
                repository.saveAll(toSave);
            }
        }

        log.info("Autores recuperados: {}/{}", recovered, candidates.size());
        return recovered;
    }

    private void enrichBatch(List<ExtractedBookEntity> batch) {
        String booksJson = serializeBatch(batch);
        List<SonnetEnrichment> enrichments = claudeResponseParser.parseEnrichment(booksJson);

        for (int i = 0; i < batch.size(); i++) {
            ExtractedBookEntity entity = batch.get(i);
            SonnetEnrichment enrichment = i < enrichments.size()
                    ? enrichments.get(i)
                    : new SonnetEnrichment(null, null, entity.isSaga());
            applyEnrichment(entity, enrichment);
        }

        repository.saveAll(batch);
    }

    private void applyEnrichment(ExtractedBookEntity entity, SonnetEnrichment enrichment) {
        if (enrichment.authorCorrected() != null) {
            entity.setAuthorCorrected(enrichment.authorCorrected());
            if (entity.getAuthor() == null || entity.getAuthor().isBlank()) {
                entity.setAuthor(enrichment.authorCorrected());
            }
        }
        if (enrichment.isSaga() != entity.isSaga()) {
            entity.setSaga(enrichment.isSaga());
        }

        if (enrichment.titleEs() != null) {
            spanishTitleResolver.enrichWithSonnetTitle(entity, enrichment.titleEs());
        } else {
            spanishTitleResolver.enrichWithOLFallback(entity);
        }

        String resolvedAuthor = entity.getAuthorCorrected();
        if (resolvedAuthor != null && !resolvedAuthor.isBlank()) {
            List<AuthorEntity> authors = AuthorNameParser.parse(resolvedAuthor).stream()
                    .map(authorEnricher::findOrCreate)
                    .toList();
            entity.getAuthors().addAll(authors);
        }

        entity.setEnriched(true);
        entity.setEnrichedAt(Instant.now());
    }

    private String serializeBatch(List<ExtractedBookEntity> batch) {
        try {
            List<BookInput> inputs = batch.stream()
                    .map(e -> new BookInput(e.getTitle(), e.getAuthor(), e.isSaga()))
                    .toList();
            return objectMapper.writeValueAsString(inputs);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String serializeBatchForLookup(List<ExtractedBookEntity> batch) {
        try {
            List<BookLookupInput> inputs = batch.stream()
                    .map(e -> new BookLookupInput(e.getTitle(), e.isSaga()))
                    .toList();
            return objectMapper.writeValueAsString(inputs);
        } catch (Exception e) {
            return "[]";
        }
    }

    private record BookInput(String title, String author, boolean isSaga) {}

    private record BookLookupInput(String title, boolean isSaga) {}
}
