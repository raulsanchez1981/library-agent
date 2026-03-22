package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.extractor.ClaudeGateway;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.extractor.OpenLibraryClient;
import com.libraryagent.ingestion.extractor.SpanishEdition;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enriquece los libros extraídos con título en español y autor corregido.
 * Independiente de la fuente: funciona igual para Reddit, Instagram, RSS u otras.
 *
 * Flujo por libro:
 * - Sonnet propone titleEs y authorCorrected en batches de 10
 * - Si Sonnet tiene titleEs: verificar contra OpenLibrary buscando por título español
 *   → CONFIRMED si OL coincide, REVIEW si difiere, SONNET_ONLY si OL no encuentra nada
 * - Si Sonnet devuelve null: buscar en OL por título inglés con lang=spa
 *   → OL_ONLY si OL lo encuentra, NONE si no
 */
@Service
public class BookEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(BookEnrichmentService.class);
    private static final int BATCH_SIZE = 10;
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.6;
    private static final Set<String> STOPWORDS = Set.of(
            "el", "la", "los", "las", "de", "del", "en", "un", "una",
            "the", "a", "an", "and", "of", "in"
    );

    private final ExtractedBookRepository repository;
    private final ClaudeGateway claudeGateway;
    private final OpenLibraryClient openLibraryClient;
    private final ObjectMapper objectMapper;

    public BookEnrichmentService(
            ExtractedBookRepository repository,
            ClaudeGateway claudeGateway,
            OpenLibraryClient openLibraryClient,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.claudeGateway = claudeGateway;
        this.openLibraryClient = openLibraryClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Devuelve el número de libros pendientes de enriquecer.
     */
    public long countPendingEnrichment() {
        return repository.countByEnrichedFalse();
    }

    /**
     * Procesa todos los libros con enriched=false en batches de 10.
     */
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

        log.info("Enriquecimiento completado");
    }

    private void enrichBatch(List<ExtractedBookEntity> batch) {
        String booksJson = serializeBatch(batch);
        List<SonnetEnrichment> enrichments = callSonnetAndParse(booksJson);

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
        }
        if (enrichment.isSaga() != entity.isSaga()) {
            entity.setSaga(enrichment.isSaga());
        }

        String sonnetTitleEs = enrichment.titleEs();

        if (sonnetTitleEs != null) {
            enrichWithSonnetTitle(entity, sonnetTitleEs);
        } else {
            enrichWithOLFallback(entity);
        }

        entity.setEnriched(true);
        entity.setEnrichedAt(Instant.now());
    }

    private void enrichWithSonnetTitle(ExtractedBookEntity entity, String sonnetTitleEs) {
        Optional<SpanishEdition> olResult = openLibraryClient.findBySpanishTitle(sonnetTitleEs);

        if (olResult.isPresent() && olResult.get().titleEs() != null) {
            String olTitleEs = olResult.get().titleEs();
            if (titlesSimilar(sonnetTitleEs, olTitleEs)) {
                entity.setTitleEs(sonnetTitleEs);
                entity.setEnrichmentSource(EnrichmentSource.CONFIRMED);
                entity.setAvailableInSpanish(true);
            } else {
                entity.setTitleEs(sonnetTitleEs);
                entity.setTitleEsOl(olTitleEs);
                entity.setEnrichmentSource(EnrichmentSource.REVIEW);
                entity.setAvailableInSpanish(true);
            }
        } else {
            entity.setTitleEs(sonnetTitleEs);
            entity.setEnrichmentSource(EnrichmentSource.SONNET_ONLY);
            entity.setAvailableInSpanish(false);
        }
    }

    private void enrichWithOLFallback(ExtractedBookEntity entity) {
        Optional<SpanishEdition> olResult = openLibraryClient.findSpanishEdition(entity.getTitle());

        if (olResult.isPresent() && olResult.get().titleEs() != null) {
            SpanishEdition ol = olResult.get();
            entity.setTitleEs(ol.titleEs());
            entity.setEnrichmentSource(EnrichmentSource.OL_ONLY);
            entity.setAvailableInSpanish(true);
            if (entity.getAuthor() == null && ol.author() != null) {
                entity.setAuthorCorrected(ol.author());
            }
        } else {
            entity.setEnrichmentSource(EnrichmentSource.NONE);
            entity.setAvailableInSpanish(false);
        }
    }

    /**
     * Compara dos títulos en español ignorando diferencias de capitalización,
     * artículos y signos de puntuación.
     */
    private boolean titlesSimilar(String a, String b) {
        Set<String> tokensA = significantTokens(a);
        Set<String> tokensB = significantTokens(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return false;
        long common = tokensA.stream().filter(tokensB::contains).count();
        double similarity = (double) common / Math.max(tokensA.size(), tokensB.size());
        return similarity >= TITLE_SIMILARITY_THRESHOLD;
    }

    private Set<String> significantTokens(String title) {
        if (title == null) return Set.of();
        String normalized = title.toLowerCase().replaceAll("[^a-záéíóúüñ0-9 ]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank() && !STOPWORDS.contains(t) && t.length() > 1)
                .collect(Collectors.toSet());
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

    private List<SonnetEnrichment> callSonnetAndParse(String booksJson) {
        try {
            String rawJson = claudeGateway.enrichBooksBatchJson(booksJson);
            String clean = stripMarkdownFences(rawJson);
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

    private String stripMarkdownFences(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("```json\\s*", "").replaceAll("```", "").strip();
        }
        return trimmed;
    }

    // --- Tipos internos ---

    private record BookInput(String title, String author, boolean isSaga) {}

    private record SonnetEnrichment(String titleEs, String authorCorrected, boolean isSaga) {}
}
