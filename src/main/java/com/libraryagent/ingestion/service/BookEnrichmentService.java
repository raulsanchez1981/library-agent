package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.extractor.ClaudeGateway;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.extractor.OpenLibraryClient;
import com.libraryagent.ingestion.extractor.SpanishEdition;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
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
 * - Si Sonnet tiene titleEs: llamar a OL por título español para obtener sugerencia
 *   → SONNET + HIGH si OL confirma con palabras clave similares
 *   → SONNET + MEDIUM si OL no encuentra nada
 *   → SONNET + LOW si OL devuelve título con baja similitud de palabras clave
 * - Si Sonnet devuelve null: buscar en OL por título inglés con lang=spa
 *   → OL_ONLY (confidence=null) si OL lo encuentra, NONE (confidence=null) si no
 */
@Service
public class BookEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(BookEnrichmentService.class);
    private static final int BATCH_SIZE = 10;
    private static final double KEYWORD_SIMILARITY_THRESHOLD = 0.6;
    private static final int MIN_KEYWORD_LENGTH = 4;
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this",
            "los", "las", "del", "una", "con", "por", "que", "sus"
    );

    private final ExtractedBookRepository repository;
    private final AuthorRepository authorRepository;
    private final ClaudeGateway claudeGateway;
    private final OpenLibraryClient openLibraryClient;
    private final ObjectMapper objectMapper;
    private final ExtractedBookAdminService extractedBookAdminService;

    public BookEnrichmentService(
            ExtractedBookRepository repository,
            AuthorRepository authorRepository,
            ClaudeGateway claudeGateway,
            OpenLibraryClient openLibraryClient,
            ObjectMapper objectMapper,
            ExtractedBookAdminService extractedBookAdminService) {
        this.repository = repository;
        this.authorRepository = authorRepository;
        this.claudeGateway = claudeGateway;
        this.openLibraryClient = openLibraryClient;
        this.objectMapper = objectMapper;
        this.extractedBookAdminService = extractedBookAdminService;
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

        extractedBookAdminService.linkUnverifiedBooks();
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
            // Si author está vacío, el valor corregido de Sonnet es la mejor fuente disponible
            if (entity.getAuthor() == null || entity.getAuthor().isBlank()) {
                entity.setAuthor(enrichment.authorCorrected());
            }
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

        // Crear o recuperar entidades Author a partir del campo authorCorrected ya establecido
        String resolvedAuthor = entity.getAuthorCorrected();
        if (resolvedAuthor != null && !resolvedAuthor.isBlank()) {
            List<AuthorEntity> authors = AuthorNameParser.parse(resolvedAuthor).stream()
                    .map(this::findOrCreateAuthor)
                    .toList();
            entity.getAuthors().addAll(authors);
        }

        entity.setEnriched(true);
        entity.setEnrichedAt(Instant.now());
    }

    private AuthorEntity findOrCreateAuthor(String name) {
        String normalized = TitleCaseNormalizer.normalize(name);
        return authorRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> authorRepository.save(new AuthorEntity(normalized)));
    }

    private void enrichWithSonnetTitle(ExtractedBookEntity entity, String sonnetTitleEs) {
        Optional<SpanishEdition> olResult = openLibraryClient.findBySpanishTitle(sonnetTitleEs);

        entity.setTitleEs(sonnetTitleEs);
        entity.setEnrichmentSource(EnrichmentSource.SONNET);
        entity.setAvailableInSpanish(true);

        if (olResult.isPresent() && olResult.get().titleEs() != null) {
            String olTitleEs = olResult.get().titleEs();
            entity.setTitleEsOl(olTitleEs);
            double similarity = keywordSimilarity(sonnetTitleEs, olTitleEs);
            entity.setConfidence(similarity >= KEYWORD_SIMILARITY_THRESHOLD ? Confidence.HIGH : Confidence.LOW);
        } else {
            entity.setConfidence(Confidence.MEDIUM);
        }
    }

    private void enrichWithOLFallback(ExtractedBookEntity entity) {
        Optional<SpanishEdition> olResult = openLibraryClient.findSpanishEdition(entity.getTitle());

        if (olResult.isPresent() && olResult.get().titleEs() != null) {
            SpanishEdition ol = olResult.get();
            entity.setTitleEs(ol.titleEs());
            entity.setEnrichmentSource(EnrichmentSource.OL_ONLY);
            entity.setConfidence(null);
            entity.setAvailableInSpanish(true);
            if (ol.author() != null && (entity.getAuthor() == null || entity.getAuthor().isBlank())) {
                entity.setAuthorCorrected(ol.author());
                entity.setAuthor(ol.author());
            }
        } else {
            entity.setEnrichmentSource(EnrichmentSource.NONE);
            entity.setConfidence(null);
            entity.setAvailableInSpanish(false);
        }
    }

    /**
     * Re-enriquece libros que ya fueron procesados pero quedaron sin autor.
     * Usa lookupAuthorsBatchJson para preguntar a Sonnet solo por el autor,
     * enviando únicamente title e isSaga (sin author) para evitar que Sonnet
     * interprete author=null como "no hay autor" y descarte la búsqueda.
     *
     * @return número de autores recuperados
     */
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
            List<String> authors = callLookupAuthorsAndParse(booksJson);

            List<ExtractedBookEntity> toSave = new ArrayList<>();
            for (int j = 0; j < batch.size(); j++) {
                ExtractedBookEntity entity = batch.get(j);
                String author = j < authors.size() ? authors.get(j) : null;
                if (author != null) {
                    entity.setAuthor(author);
                    entity.setAuthorCorrected(author);
                    List<AuthorEntity> authorEntities = AuthorNameParser.parse(author).stream()
                            .map(this::findOrCreateAuthor)
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

    /**
     * Calcula similitud entre dos cadenas basándose en palabras clave de más de 4 letras.
     * Excluye artículos y preposiciones comunes.
     * @return ratio palabras_comunes / max(palabras_a, palabras_b), entre 0.0 y 1.0
     */
    private double keywordSimilarity(String a, String b) {
        Set<String> keywordsA = keywords(a);
        Set<String> keywordsB = keywords(b);
        if (keywordsA.isEmpty() || keywordsB.isEmpty()) return 0.0;
        long common = keywordsA.stream().filter(keywordsB::contains).count();
        return (double) common / Math.max(keywordsA.size(), keywordsB.size());
    }

    private Set<String> keywords(String text) {
        if (text == null) return Set.of();
        String normalized = text.toLowerCase().replaceAll("[^a-záéíóúüñ0-9 ]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank() && t.length() > MIN_KEYWORD_LENGTH && !STOPWORDS.contains(t))
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

    private List<String> callLookupAuthorsAndParse(String booksJson) {
        try {
            String rawJson = claudeGateway.lookupAuthorsBatchJson(booksJson);
            String clean = stripMarkdownFences(rawJson);
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

    // --- Tipos internos ---

    private record BookInput(String title, String author, boolean isSaga) {}

    private record BookLookupInput(String title, boolean isSaga) {}

    private record SonnetEnrichment(String titleEs, String authorCorrected, boolean isSaga) {}
}
