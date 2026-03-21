package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.extractor.BookTitleExtractor;
import com.libraryagent.ingestion.model.ExtractedBook;
import com.libraryagent.ingestion.model.RawMention;
import com.libraryagent.ingestion.sources.BookSourceIngester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final List<BookSourceIngester> ingesters;
    private final BookTitleExtractor extractor;

    public IngestionService(List<BookSourceIngester> ingesters, BookTitleExtractor extractor) {
        this.ingesters = ingesters;
        this.extractor = extractor;
    }

    /**
     * Ejecuta el ciclo completo de ingesta para todas las fuentes disponibles.
     * Retorna los libros extraídos y listos para scoring.
     */
    public List<ExtractedBook> runFullIngestion() {
        List<RawMention> mentions = ingesters.stream()
                .filter(BookSourceIngester::isAvailable)
                .peek(ingester -> log.info("Ingiriendo desde fuente: {}", ingester.sourceId()))
                .flatMap(ingester -> ingester.ingest().stream())
                .toList();

        log.info("Menciones recogidas: {}", mentions.size());

        List<ExtractedBook> books = extractor.extractBatch(mentions);

        log.info("Libros extraídos: {}", books.size());
        return books;
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
}
