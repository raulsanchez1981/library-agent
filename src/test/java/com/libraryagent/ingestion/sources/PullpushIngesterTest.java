package com.libraryagent.ingestion.sources;

import com.libraryagent.ingestion.PullpushProperties;
import com.libraryagent.ingestion.model.RawMention;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullpushIngesterTest {

    @Mock
    PullpushApiClient pullpushApiClient;

    PullpushIngester ingester;

    @BeforeEach
    void setUp() {
        PullpushProperties properties = new PullpushProperties(
                List.of("Fantasy", "printSF"),
                25
        );
        ingester = new PullpushIngester(pullpushApiClient, properties);
    }

    @Test
    void shouldReturnMentionsWhenPostsFetched() {
        // Given
        List<PullpushPost> posts = List.of(
                new PullpushPost(
                        "Best fantasy books of 2026",
                        "Here is my list of recommendations...",
                        "https://www.reddit.com/r/Fantasy/comments/abc123/best_fantasy/",
                        "Fantasy",
                        Instant.parse("2026-03-21T08:00:00Z")
                )
        );
        when(pullpushApiClient.fetchPosts("Fantasy")).thenReturn(posts);
        when(pullpushApiClient.fetchPosts("printSF")).thenReturn(List.of());

        // When
        List<RawMention> result = ingester.ingest();

        // Then
        assertThat(result).hasSize(1);
        RawMention mention = result.getFirst();
        assertThat(mention.source()).isEqualTo("reddit/Fantasy");
        assertThat(mention.text()).contains("Best fantasy books of 2026");
        assertThat(mention.text()).contains("Here is my list of recommendations...");
        assertThat(mention.url()).isEqualTo("https://www.reddit.com/r/Fantasy/comments/abc123/best_fantasy/");
        assertThat(mention.fetchedAt()).isEqualTo(Instant.parse("2026-03-21T08:00:00Z"));
    }

    @Test
    void shouldReturnEmptyListWhenNoPostsFound() {
        // Given
        when(pullpushApiClient.fetchPosts(anyString())).thenReturn(List.of());

        // When
        List<RawMention> result = ingester.ingest();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldContinueIngestingWhenOneSubredditFails() {
        // Given
        when(pullpushApiClient.fetchPosts("Fantasy"))
                .thenThrow(new RuntimeException("Connection timeout"));
        when(pullpushApiClient.fetchPosts("printSF"))
                .thenReturn(List.of(new PullpushPost(
                        "Hard sci-fi recommendations",
                        "Looking for books similar to The Expanse",
                        "https://www.reddit.com/r/printSF/comments/xyz789/",
                        "printSF",
                        Instant.parse("2026-03-21T09:00:00Z")
                )));

        // When
        List<RawMention> result = ingester.ingest();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().source()).isEqualTo("reddit/printSF");
    }

    @Test
    void shouldConcatenateTitleAndSelftextWithSpace() {
        // Given
        when(pullpushApiClient.fetchPosts("Fantasy")).thenReturn(List.of(
                new PullpushPost("My title", "My body text", "http://url", "Fantasy", Instant.now())
        ));
        when(pullpushApiClient.fetchPosts("printSF")).thenReturn(List.of());

        // When
        List<RawMention> result = ingester.ingest();

        // Then
        assertThat(result.getFirst().text()).isEqualTo("My title My body text");
    }

    @Test
    void shouldAlwaysBeAvailable() {
        assertThat(ingester.isAvailable()).isTrue();
    }

    @Test
    void shouldReturnMentionsFromAllSubreddits() {
        // Given
        when(pullpushApiClient.fetchPosts("Fantasy")).thenReturn(List.of(
                new PullpushPost("Fantasy post", "", "http://url1", "Fantasy", Instant.now())
        ));
        when(pullpushApiClient.fetchPosts("printSF")).thenReturn(List.of(
                new PullpushPost("SciFi post", "", "http://url2", "printSF", Instant.now())
        ));

        // When
        List<RawMention> result = ingester.ingest();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(RawMention::source)
                .containsExactlyInAnyOrder("reddit/Fantasy", "reddit/printSF");
    }
}
