package com.libraryagent.ingestion.extractor;

import com.libraryagent.ingestion.model.ExtractedBook;
import com.libraryagent.ingestion.model.RawMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Extrae títulos y autores de libros a partir de texto no estructurado usando Claude API.
 */
@Service
public class BookTitleExtractor {

    private static final Logger log = LoggerFactory.getLogger(BookTitleExtractor.class);

    @Value("${claude.api-key:#{null}}")
    private String claudeApiKey;

    @Value("${claude.model:claude-opus-4-6}")
    private String claudeModel;

    /**
     * Analiza el texto de una mención e intenta extraer un libro.
     * Retorna empty si no se detecta ningún libro con suficiente confianza.
     */
    public Optional<ExtractedBook> extract(RawMention mention) {
        // TODO: implementar llamada a Claude API
        // El prompt debe solicitar JSON: { "title": "...", "author": "..." }
        // Si Claude no detecta un libro claro, responde { "title": null }
        log.warn("BookTitleExtractor no implementado aún para mención: {}", mention.id());
        return Optional.empty();
    }

    /**
     * Procesa un lote de menciones. Útil para reducir latencia en ingesta masiva.
     */
    public List<ExtractedBook> extractBatch(List<RawMention> mentions) {
        return mentions.stream()
                .map(this::extract)
                .flatMap(Optional::stream)
                .toList();
    }
}
