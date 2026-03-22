package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "live", matches = "true")
class BookEnrichmentLiveTest {

    @Autowired
    private BookEnrichmentService bookEnrichmentService;

    @Autowired
    private ExtractedBookRepository extractedBookRepository;

    @Test
    void shouldReEnrichAuthorsForBooksWithNullAuthor() {
        long nullBefore = extractedBookRepository.findByEnrichedTrueAndAuthorIsNull().size();
        System.out.printf("%nAutores null antes: %d%n", nullBefore);

        int recuperados = bookEnrichmentService.reEnrichAuthors();

        long nullAfter = extractedBookRepository.findByEnrichedTrueAndAuthorIsNull().size();
        System.out.printf("Autores recuperados: %d%n", recuperados);
        System.out.printf("Autores null después: %d%n", nullAfter);
    }

    @Test
    void shouldEnrichPendingBooksWithRealServices() {
        bookEnrichmentService.enrichPending();

        List<ExtractedBookEntity> enriched = extractedBookRepository.findAll().stream()
                .filter(ExtractedBookEntity::isEnriched)
                .toList();

        System.out.println("=== Resultado de enriquecimiento ===");
        System.out.println("Total libros enriquecidos: " + enriched.size());

        Map<EnrichmentSource, List<ExtractedBookEntity>> bySource = enriched.stream()
                .collect(Collectors.groupingBy(e -> e.getEnrichmentSource() != null
                        ? e.getEnrichmentSource()
                        : EnrichmentSource.NONE));

        Arrays.stream(EnrichmentSource.values()).forEach(source -> {
            List<ExtractedBookEntity> group = bySource.getOrDefault(source, List.of());
            System.out.printf("  %-12s : %d%n", source, group.size());
            if (!group.isEmpty()) {
                group.stream().limit(5).forEach(e ->
                        System.out.printf("    - \"%s\" → \"%s\"%n", e.getTitle(), e.getTitleEs()));
            }
        });

        assertThat(enriched).isNotEmpty();
    }
}
