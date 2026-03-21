package com.libraryagent.ingestion.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.PullpushProperties;
import com.libraryagent.ingestion.extractor.AnthropicClaudeGateway;
import com.libraryagent.ingestion.extractor.BookTitleExtractor;
import com.libraryagent.ingestion.extractor.ExtractedBookResult;
import com.libraryagent.ingestion.extractor.RestClientOpenLibraryClient;
import com.libraryagent.ingestion.model.RawMention;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Test de integración manual del pipeline completo:
 * Pullpush → Claude API → OpenLibrary → ExtractedBookResult
 *
 * Ejecutar con: mvn test -Dtest=BookTitleExtractorLiveTest -Dlive=true -DfailIfNoTests=false
 * Requiere: ANTHROPIC_API_KEY en el entorno.
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class BookTitleExtractorLiveTest {

    @Test
    void shouldExtractAndEnrichBooksFromRealFantasyPosts() {
        // Given
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY no está definida en el entorno");
        }

        PullpushProperties properties = new PullpushProperties(List.of("Fantasy"), 3);
        PullpushApiClient pullpushClient =
                new RestClientPullpushApiClient(properties, RestClient.builder());
        AnthropicClaudeGateway claudeGateway = new AnthropicClaudeGateway(apiKey);
        RestClientOpenLibraryClient olClient =
                new RestClientOpenLibraryClient(RestClient.builder());
        BookTitleExtractor extractor =
                new BookTitleExtractor(claudeGateway, olClient, new ObjectMapper());

        // When
        List<PullpushPost> posts = pullpushClient.fetchPosts("Fantasy");

        System.out.println("\n=================================================================");
        System.out.println("  PIPELINE: Pullpush → Claude API → OpenLibrary");
        System.out.println("=================================================================");

        int totalBooks = 0;
        int totalConEdicionEs = 0;

        for (PullpushPost post : posts) {
            RawMention mention = new RawMention(
                    UUID.randomUUID(),
                    "reddit",
                    post.title() + " " + post.selftext(),
                    post.url(),
                    Instant.now()
            );

            List<ExtractedBookResult> books = extractor.extract(mention);

            System.out.printf("%n--- Post: %s%n", post.title());
            System.out.printf("    URL: %s%n", post.url());

            if (books.isEmpty()) {
                System.out.println("    [Sin libros detectados por Claude]");
            } else {
                for (ExtractedBookResult book : books) {
                    totalBooks++;
                    if (book.availableInSpanish()) totalConEdicionEs++;

                    System.out.printf("    Título   : %s%n", book.title());
                    System.out.printf("    Autor    : %s%n", book.author()  != null ? book.author()  : "(desconocido)");
                    System.out.printf("    En esp.  : %s%n", book.titleEs() != null ? book.titleEs() : "(sin título alternativo)");
                    System.out.printf("    OL esp.  : %s%n", book.availableInSpanish() ? "SI" : "no");
                }
            }
        }

        System.out.println("\n=================================================================");
        System.out.printf("  Posts procesados  : %d%n", posts.size());
        System.out.printf("  Libros detectados : %d%n", totalBooks);
        System.out.printf("  Con edición esp.  : %d%n", totalConEdicionEs);
        System.out.println("=================================================================\n");

        // Then — el pipeline no lanza excepciones (libros detectados puede ser 0 si los posts no mencionan títulos)
        org.assertj.core.api.Assertions.assertThat(posts).hasSize(3);
    }
}
