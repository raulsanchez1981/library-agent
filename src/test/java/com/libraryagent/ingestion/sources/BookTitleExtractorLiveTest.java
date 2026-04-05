package com.libraryagent.ingestion.sources;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.PullpushProperties;
import com.libraryagent.ingestion.extractor.AnthropicClaudeGateway;
import com.libraryagent.ingestion.extractor.BookTitleExtractor;
import com.libraryagent.ingestion.extractor.ExtractedBookResult;
import com.libraryagent.ingestion.model.RawMention;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test manual de la extracción rápida con Haiku (sin enriquecimiento).
 * Ejecutar con: mvn test -Dtest=BookTitleExtractorLiveTest -Dlive=true -DfailIfNoTests=false
 * Requiere: ANTHROPIC_API_KEY en el entorno.
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class BookTitleExtractorLiveTest {

    @Test
    void shouldExtractBooksFromRealFantasyPosts() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY no está definida en el entorno");
        }

        PullpushProperties properties = new PullpushProperties(Map.of("Fantasy", "fantasía épica"), 3);
        PullpushApiClient pullpushClient = new RestClientPullpushApiClient(properties, RestClient.builder());
        BookTitleExtractor extractor = new BookTitleExtractor(
                new AnthropicClaudeGateway(AnthropicOkHttpClient.builder().apiKey(apiKey).build()),
                new ObjectMapper());

        List<PullpushPost> posts = pullpushClient.fetchPosts("Fantasy");

        System.out.println("\n=================================================================");
        System.out.println("  EXTRACCIÓN HAIKU: Pullpush → Claude Haiku");
        System.out.println("=================================================================");

        int totalBooks = 0;
        for (PullpushPost post : posts) {
            RawMention mention = new RawMention(UUID.randomUUID(), "reddit",
                    post.title() + " " + post.selftext(), post.url(), Instant.now());
            List<ExtractedBookResult> books = extractor.extract(mention);
            totalBooks += books.size();

            System.out.printf("%n--- Post: %s%n", post.title());
            books.forEach(b -> System.out.printf("    [%s] - %s (saga: %s)%n",
                    b.title(), b.author() != null ? b.author() : "?", b.isSaga()));
        }

        System.out.printf("%nTotal libros detectados: %d%n", totalBooks);
        org.assertj.core.api.Assertions.assertThat(posts).hasSize(3);
    }
}
