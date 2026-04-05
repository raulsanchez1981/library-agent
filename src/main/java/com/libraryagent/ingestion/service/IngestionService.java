package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.extractor.BookTitleExtractor;
import com.libraryagent.ingestion.extractor.ExtractedBookResult;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.model.RawMention;
import com.libraryagent.ingestion.model.RawMentionEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.RawMentionRepository;
import com.libraryagent.ingestion.sources.BookSourceIngester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final List<BookSourceIngester> ingesters;
    private final BookTitleExtractor extractor;
    private final RawMentionRepository rawMentionRepository;
    private final ExtractedBookRepository extractedBookRepository;

    public IngestionService(
            List<BookSourceIngester> ingesters,
            BookTitleExtractor extractor,
            RawMentionRepository rawMentionRepository,
            ExtractedBookRepository extractedBookRepository) {
        this.ingesters = ingesters;
        this.extractor = extractor;
        this.rawMentionRepository = rawMentionRepository;
        this.extractedBookRepository = extractedBookRepository;
    }

    /**
     * Ejecuta el ciclo completo de ingesta para todas las fuentes disponibles.
     * Persiste menciones y libros extraídos en PostgreSQL.
     * Retorna los libros extraídos en esta ejecución.
     */
    @Transactional
    public List<ExtractedBookResult> runFullIngestion() {
        Set<String> seenUrls = new HashSet<>();
        List<RawMention> newMentions = ingesters.stream()
                .filter(BookSourceIngester::isAvailable)
                .peek(ingester -> log.info("Ingiriendo desde fuente: {}", ingester.sourceId()))
                .flatMap(ingester -> ingester.ingest().stream())
                .filter(mention -> seenUrls.add(mention.url()))
                .filter(mention -> !rawMentionRepository.existsByUrl(mention.url()))
                .toList();

        log.info("Menciones nuevas: {}", newMentions.size());

        List<ExtractedBookResult> allBooks = new ArrayList<>();
        for (RawMention mention : newMentions) {
            RawMentionEntity savedMention = rawMentionRepository.save(toEntity(mention));
            List<ExtractedBookResult> books = extractor.extract(mention);
            books.forEach(book -> {
                if (extractedBookRepository.existsByTitleIgnoreCase(book.title())) {
                    log.debug("Libro duplicado descartado: {}", book.title());
                } else {
                    extractedBookRepository.save(toEntity(book, savedMention));
                    allBooks.add(book);
                }
            });
        }

        log.info("Libros extraídos y persistidos: {}", allBooks.size());
        return allBooks;
    }

    /**
     * Ejecuta la ingesta de una única fuente por su identificador.
     */
    public List<RawMention> ingestSource(String sourceId) {
        return ingesters.stream()
                .filter(ingester -> ingester.sourceId().equals(sourceId))
                .findFirst()
                .map(BookSourceIngester::ingest)
                .orElseGet(() -> {
                    log.warn("Fuente no encontrada: {}", sourceId);
                    return List.of();
                });
    }

    private RawMentionEntity toEntity(RawMention mention) {
        RawMentionEntity entity = new RawMentionEntity();
        entity.setSource(mention.source());
        entity.setText(mention.text());
        entity.setUrl(mention.url());
        entity.setFetchedAt(mention.fetchedAt());
        return entity;
    }

    private ExtractedBookEntity toEntity(ExtractedBookResult result, RawMentionEntity sourceMention) {
        ExtractedBookEntity entity = new ExtractedBookEntity();
        entity.setTitle(result.title());
        entity.setAuthor(result.author());
        entity.setSaga(result.isSaga());
        entity.setEnriched(false);
        entity.setSourceMention(sourceMention);
        return entity;
    }
}
