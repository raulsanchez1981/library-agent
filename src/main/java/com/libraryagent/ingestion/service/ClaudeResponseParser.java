package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.extractor.ClaudeGateway;
import com.libraryagent.shared.util.MarkdownUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClaudeResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ClaudeResponseParser.class);

    private final ClaudeGateway claudeGateway;
    private final ObjectMapper objectMapper;

    public ClaudeResponseParser(ClaudeGateway claudeGateway, ObjectMapper objectMapper) {
        this.claudeGateway = claudeGateway;
        this.objectMapper = objectMapper;
    }

    public List<SonnetEnrichment> parseEnrichment(String booksJson) {
        try {
            String rawJson = claudeGateway.enrichBooksBatchJson(booksJson);
            String clean = MarkdownUtils.stripFences(rawJson);
            JsonNode array = objectMapper.readTree(clean);
            if (!array.isArray()) return List.of();

            List<SonnetEnrichment> results = new ArrayList<>();
            for (JsonNode node : array) {
                String titleEs = node.path("titleEs").isNull() ? null : node.path("titleEs").asText(null);
                String authorCorrected = node.path("authorCorrected").isNull() ? null : node.path("authorCorrected").asText(null);
                boolean isSaga = node.path("isSaga").asBoolean(false);
                results.add(new SonnetEnrichment(titleEs, authorCorrected, isSaga));
            }
            return results;
        } catch (Exception e) {
            log.warn("Error al parsear respuesta de enriquecimiento de Sonnet: {}", e.getMessage());
            return List.of();
        }
    }

    public List<String> parseAuthorLookup(String booksJson) {
        try {
            String rawJson = claudeGateway.lookupAuthorsBatchJson(booksJson);
            String clean = MarkdownUtils.stripFences(rawJson);
            JsonNode array = objectMapper.readTree(clean);
            if (!array.isArray()) return List.of();

            List<String> results = new ArrayList<>();
            for (JsonNode node : array) {
                String author = node.path("author").isNull() ? null : node.path("author").asText(null);
                results.add(author);
            }
            return results;
        } catch (Exception e) {
            log.warn("Error al parsear respuesta de lookup de autores: {}", e.getMessage());
            return List.of();
        }
    }
}
