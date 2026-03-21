package com.libraryagent.ingestion.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.model.RawMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Extrae títulos y autores de libros a partir de texto no estructurado usando Claude API,
 * y los enriquece consultando OpenLibrary para detectar edición en español.
 */
public class BookTitleExtractor {

    private static final Logger log = LoggerFactory.getLogger(BookTitleExtractor.class);

    private final ClaudeGateway claudeGateway;
    private final OpenLibraryClient openLibraryClient;
    private final ObjectMapper objectMapper;

    public BookTitleExtractor(
            ClaudeGateway claudeGateway,
            OpenLibraryClient openLibraryClient,
            ObjectMapper objectMapper) {
        this.claudeGateway = claudeGateway;
        this.openLibraryClient = openLibraryClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Procesa un lote de menciones. Útil para reducir llamadas en ingesta masiva.
     */
    public List<ExtractedBookResult> extractBatch(List<RawMention> mentions) {
        return mentions.stream()
                .flatMap(mention -> extract(mention).stream())
                .toList();
    }

    /**
     * Analiza el texto de una mención y extrae todos los libros detectados por Claude,
     * enriquecidos con datos de OpenLibrary.
     */
    public List<ExtractedBookResult> extract(RawMention mention) {
        String rawJson = claudeGateway.extractTitlesJson(mention.text());
        List<String> titles = parseTitles(rawJson);
        return titles.stream()
                .map(this::enrich)
                .toList();
    }

    private List<String> parseTitles(String rawJson) {
        try {
            String trimmed = rawJson.strip();
            // Claude a veces envuelve el JSON en ```json ... ```
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.replaceAll("```json\\s*", "").replaceAll("```", "").strip();
            }
            return objectMapper.readValue(
                    trimmed,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("No se pudo parsear respuesta de Claude: {}", rawJson);
            return List.of();
        }
    }

    private ExtractedBookResult enrich(String title) {
        Optional<SpanishEdition> edition = openLibraryClient.findSpanishEdition(title);
        return edition
                .map(ed -> new ExtractedBookResult(title, ed.author(), ed.titleEs(), ed.available()))
                .orElse(new ExtractedBookResult(title, null, null, false));
    }
}
