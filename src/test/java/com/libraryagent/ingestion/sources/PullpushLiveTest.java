package com.libraryagent.ingestion.sources;

import com.libraryagent.ingestion.PullpushProperties;
import com.libraryagent.ingestion.extractor.OpenLibraryClient;
import com.libraryagent.ingestion.extractor.RestClientOpenLibraryClient;
import com.libraryagent.ingestion.extractor.SpanishEdition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración manual contra APIs reales.
 * Ejecutar con: mvn test -Dtest=PullpushLiveTest -Dlive=true -DfailIfNoTests=false
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class PullpushLiveTest {

    @Test
    void shouldFetchRealPostsFromFantasy() {
        PullpushProperties properties = new PullpushProperties(Map.of("Fantasy", "fantasía épica"), 25);
        PullpushApiClient client = new RestClientPullpushApiClient(properties, RestClient.builder());

        List<PullpushPost> posts = client.fetchPosts("Fantasy");

        System.out.println("\n===== PULLPUSH LIVE TEST — r/Fantasy =====");
        posts.stream().limit(5).forEach(post -> {
            String fragmento = post.selftext().isBlank() ? "[sin texto]"
                    : post.selftext().substring(0, Math.min(120, post.selftext().length())).replace("\n", " ");
            System.out.printf("%n[%s]%n  %s%n  %s%n", post.title(), fragmento, post.url());
        });
        System.out.printf("%nTotal posts recibidos: %d%n", posts.size());

        assertThat(posts).isNotEmpty();
    }

    @Test
    void shouldFetchRedditPostsAndEnrichWithSpanishEdition() {
        // Given
        PullpushProperties pullpushProps = new PullpushProperties(
                Map.of("Fantasy", "fantasía épica", "suggestmeabook", "literatura general"), 10);
        PullpushApiClient pullpushClient =
                new RestClientPullpushApiClient(pullpushProps, RestClient.builder());
        OpenLibraryClient olClient =
                new RestClientOpenLibraryClient(RestClient.builder(), "https://openlibrary.org");

        // When — para cada subreddit, enriquecer los posts con OpenLibrary
        List<String> subreddits = pullpushProps.subreddits().keySet().stream().toList();
        System.out.println("\n============================================================");
        System.out.println("  PIPELINE: Reddit → OpenLibrary (edición en español)");
        System.out.println("============================================================");

        int totalPosts = 0;
        int totalConEdicionEs = 0;

        for (String subreddit : subreddits) {
            List<PullpushPost> posts = pullpushClient.fetchPosts(subreddit);
            System.out.printf("%n--- r/%s (%d posts) ---%n", subreddit, posts.size());

            for (PullpushPost post : posts) {
                Optional<SpanishEdition> edition = olClient.findSpanishEdition(post.title());

                boolean disponible = edition.map(SpanishEdition::available).orElse(false);
                String titleEs = edition.map(SpanishEdition::titleEs).orElse(null);
                String author   = edition.map(SpanishEdition::author).orElse(null);

                if (disponible) totalConEdicionEs++;
                totalPosts++;

                System.out.printf("%n  [%s]%n", post.title());
                if (author   != null) System.out.printf("  Autor   : %s%n", author);
                if (titleEs  != null) System.out.printf("  En esp. : %s%n", titleEs);
                System.out.printf("  OL esp. : %s%n", disponible ? "SI" : "no");
                System.out.printf("  URL     : %s%n", post.url());
            }
        }

        System.out.println("\n============================================================");
        System.out.printf("  Total posts procesados : %d%n", totalPosts);
        System.out.printf("  Con edicion en espanol : %d%n", totalConEdicionEs);
        System.out.println("============================================================\n");

        // Then
        assertThat(totalPosts).isPositive();
    }
}
