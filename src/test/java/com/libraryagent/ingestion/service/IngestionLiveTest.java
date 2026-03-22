package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.RawMentionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de integración manual del pipeline completo contra servicios reales:
 * Pullpush API → Claude API → OpenLibrary → PostgreSQL (docker-compose)
 *
 * Ejecutar con: mvn test -Dtest=IngestionLiveTest -Dlive=true -DfailIfNoTests=false
 * Requiere: ANTHROPIC_API_KEY en el entorno y docker-compose levantado.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "live", matches = "true")
class IngestionLiveTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private RawMentionRepository rawMentionRepository;

    @Autowired
    private ExtractedBookRepository extractedBookRepository;

    @Test
    void shouldIngestAndPersistBooksFromRealSources() {
        System.out.println("\n=================================================================");
        System.out.println("  LIVE TEST: pipeline completo con persistencia en PostgreSQL");
        System.out.println("=================================================================");

        long mentionsBefore = rawMentionRepository.count();
        long booksBefore = extractedBookRepository.count();
        System.out.printf("%nEstado inicial — raw_mentions: %d | extracted_books: %d%n",
                mentionsBefore, booksBefore);

        var books = ingestionService.runFullIngestion();

        long mentionsAfter = rawMentionRepository.count();
        long booksAfter = extractedBookRepository.count();

        System.out.println("\n=================================================================");
        System.out.printf("  Libros extraídos en esta ejecución : %d%n", books.size());
        System.out.printf("  raw_mentions  antes / después      : %d / %d (+%d)%n",
                mentionsBefore, mentionsAfter, mentionsAfter - mentionsBefore);
        System.out.printf("  extracted_books antes / después    : %d / %d (+%d)%n",
                booksBefore, booksAfter, booksAfter - booksBefore);
        System.out.println("=================================================================\n");

        books.forEach(book -> {
            System.out.printf("  [%s]%n", book.title());
            if (book.author() != null) System.out.printf("    Autor  : %s%n", book.author());
            System.out.printf("    Saga   : %s%n", book.isSaga() ? "SI" : "no");
        });

        org.assertj.core.api.Assertions.assertThat(mentionsAfter)
                .as("El pipeline completó sin errores (sin nuevas menciones si ya estaban todas procesadas)")
                .isGreaterThanOrEqualTo(mentionsBefore);
    }
}
