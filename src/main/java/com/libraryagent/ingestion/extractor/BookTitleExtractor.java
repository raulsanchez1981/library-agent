package com.libraryagent.ingestion.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.model.RawMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracción rápida y barata: una sola llamada a Claude Haiku por RawMention.
 * Devuelve libros con título, autor e isSaga, sin enriquecimiento.
 * El enriquecimiento (traducción, verificación OL) lo realiza BookEnrichmentService.
 */
public class BookTitleExtractor {

    private static final Logger log = LoggerFactory.getLogger(BookTitleExtractor.class);

    private final ClaudeGateway claudeGateway;
    private final ObjectMapper objectMapper;

    public BookTitleExtractor(ClaudeGateway claudeGateway, ObjectMapper objectMapper) {
        this.claudeGateway = claudeGateway;
        this.objectMapper = objectMapper;
    }

    public List<ExtractedBookResult> extract(RawMention mention) {
        String rawJson = claudeGateway.extractBooksJson(mention.text());
        return parseBooks(rawJson);
    }

    private List<ExtractedBookResult> parseBooks(String rawJson) {
        try {
            JsonNode array = objectMapper.readTree(stripMarkdownFences(rawJson));
            if (!array.isArray()) return List.of();

            List<ExtractedBookResult> results = new ArrayList<>();
            for (JsonNode node : array) {
                String title = node.path("title").asText(null);
                if (title == null || title.isBlank()) continue;

                String author = node.path("author").isNull() ? null : node.path("author").asText(null);
                boolean isSaga = node.path("isSaga").asBoolean(false);
                results.add(new ExtractedBookResult(title, author, isSaga));
            }
            return results;

        } catch (Exception e) {
            log.warn("No se pudo parsear respuesta de Haiku: {}", rawJson);
            return List.of();
        }
    }

    private String stripMarkdownFences(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("```json\\s*", "").replaceAll("```", "").strip();
        }
        return trimmed;
    }
}
