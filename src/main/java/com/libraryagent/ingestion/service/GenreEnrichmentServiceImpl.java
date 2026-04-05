package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.ClaudeGateway;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GenreEnrichmentServiceImpl implements GenreEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(GenreEnrichmentServiceImpl.class);
    private static final int BATCH_SIZE = 10;

    private final VerifiedTitleRepository verifiedTitleRepository;
    private final ExtractedBookRepository extractedBookRepository;
    private final ClaudeGateway claudeGateway;
    private final GenreService genreService;
    private final ObjectMapper objectMapper;

    public GenreEnrichmentServiceImpl(
            VerifiedTitleRepository verifiedTitleRepository,
            ExtractedBookRepository extractedBookRepository,
            ClaudeGateway claudeGateway,
            GenreService genreService,
            ObjectMapper objectMapper) {
        this.verifiedTitleRepository = verifiedTitleRepository;
        this.extractedBookRepository = extractedBookRepository;
        this.claudeGateway = claudeGateway;
        this.genreService = genreService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int enrichMissingGenres() {
        List<VerifiedTitleEntity> targets = verifiedTitleRepository.findAllWithoutGenres();
        log.info("Enriquecimiento de géneros (faltantes): {} títulos sin géneros", targets.size());
        return processInBatches(targets);
    }

    @Override
    @Async("cdlSearchExecutor")
    @Transactional
    public void enrichSingle(VerifiedTitleEntity verifiedTitle) {
        log.info("Enriquecimiento de géneros para: {}", verifiedTitle.getName());
        enrichBatch(List.of(verifiedTitle));
    }

    @Override
    @Async("cdlSearchExecutor")
    @Transactional
    public void enrichAllGenres() {
        List<VerifiedTitleEntity> targets = verifiedTitleRepository.findAllWithGenres();
        log.info("Enriquecimiento de géneros (todos): {} títulos", targets.size());
        processInBatches(targets);
    }

    private int processInBatches(List<VerifiedTitleEntity> titles) {
        int updated = 0;
        for (int i = 0; i < titles.size(); i += BATCH_SIZE) {
            List<VerifiedTitleEntity> batch = titles.subList(i, Math.min(i + BATCH_SIZE, titles.size()));
            updated += enrichBatch(batch);
        }
        log.info("Enriquecimiento de géneros completado: {} títulos actualizados", updated);
        return updated;
    }

    private int enrichBatch(List<VerifiedTitleEntity> batch) {
        String booksJson = serializeBatch(batch);
        List<List<String>> genresBatch = callHaikuAndParse(booksJson);

        int updated = 0;
        for (int i = 0; i < batch.size(); i++) {
            VerifiedTitleEntity vt = batch.get(i);
            List<String> inferredGenres = i < genresBatch.size() ? genresBatch.get(i) : List.of();
            if (applyGenres(vt, inferredGenres)) {
                verifiedTitleRepository.save(vt);
                updated++;
            }
        }
        return updated;
    }

    /**
     * Aplica los géneros inferidos al título, evitando duplicados.
     * @return true si se añadió al menos un género nuevo
     */
    private boolean applyGenres(VerifiedTitleEntity vt, List<String> inferredGenres) {
        if (inferredGenres.isEmpty()) return false;

        List<String> existingNames = vt.getGenres().stream()
                .map(GenreEntity::getName)
                .map(String::toLowerCase)
                .toList();

        List<GenreEntity> toAdd = inferredGenres.stream()
                .filter(name -> !existingNames.contains(name.toLowerCase()))
                .map(genreService::findOrCreate)
                .toList();

        if (toAdd.isEmpty()) return false;

        vt.getGenres().addAll(toAdd);
        return true;
    }

    private String serializeBatch(List<VerifiedTitleEntity> batch) {
        try {
            List<BookGenreInput> inputs = batch.stream()
                    .map(vt -> {
                        String author = extractAuthor(vt);
                        return new BookGenreInput(vt.getName(), author, vt.getSynopsis());
                    })
                    .toList();
            return objectMapper.writeValueAsString(inputs);
        } catch (Exception e) {
            log.warn("Error serializando batch para inferencia de géneros: {}", e.getMessage());
            return "[]";
        }
    }

    private String extractAuthor(VerifiedTitleEntity vt) {
        return extractedBookRepository
                .findByVerifiedTitleIdWithSearchData(vt.getId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(book -> book.getAuthorCorrected() != null ? book.getAuthorCorrected() : book.getAuthor())
                .orElse(null);
    }

    private List<List<String>> callHaikuAndParse(String booksJson) {
        try {
            String rawJson = claudeGateway.inferGenresBatchJson(booksJson);
            String clean = stripMarkdownFences(rawJson);
            JsonNode array = objectMapper.readTree(clean);
            if (!array.isArray()) return List.of();

            List<List<String>> results = new ArrayList<>();
            for (JsonNode node : array) {
                JsonNode genresNode = node.path("genres");
                if (genresNode.isArray()) {
                    List<String> genres = new ArrayList<>();
                    genresNode.forEach(g -> {
                        String genre = g.asText(null);
                        if (genre != null && !genre.isBlank()) genres.add(genre);
                    });
                    results.add(List.copyOf(genres));
                } else {
                    results.add(List.of());
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Error al parsear respuesta de inferencia de géneros: {}", e.getMessage());
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

    private record BookGenreInput(String title, String author, String synopsis) {}
}
