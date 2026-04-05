package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.extractor.OpenLibraryClient;
import com.libraryagent.ingestion.extractor.SpanishEdition;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SpanishTitleResolver {

    private static final double KEYWORD_SIMILARITY_THRESHOLD = 0.6;
    private static final int MIN_KEYWORD_LENGTH = 4;
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this",
            "los", "las", "del", "una", "con", "por", "que", "sus"
    );

    private final OpenLibraryClient openLibraryClient;

    public SpanishTitleResolver(OpenLibraryClient openLibraryClient) {
        this.openLibraryClient = openLibraryClient;
    }

    public void enrichWithSonnetTitle(ExtractedBookEntity entity, String sonnetTitleEs) {
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

    public void enrichWithOLFallback(ExtractedBookEntity entity) {
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
}
