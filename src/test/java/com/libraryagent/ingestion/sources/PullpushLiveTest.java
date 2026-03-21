package com.libraryagent.ingestion.sources;

import com.libraryagent.ingestion.PullpushProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración manual contra la API real de Pullpush.
 * Deshabilitado por defecto para no ejecutarse en CI.
 *
 * Ejecutar manualmente con:
 *   mvn test -pl . -Dtest=PullpushLiveTest -Dlive=true -DfailIfNoTests=false
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class PullpushLiveTest {

    @Test
    void shouldFetchRealPostsFromFantasy() {
        // Given
        PullpushProperties properties = new PullpushProperties(List.of("Fantasy"), 25);
        PullpushApiClient client = new RestClientPullpushApiClient(properties, RestClient.builder());

        // When
        List<PullpushPost> posts = client.fetchPosts("Fantasy");

        // Then — imprimir los 5 primeros
        System.out.println("\n===== PULLPUSH LIVE TEST — r/Fantasy =====");
        posts.stream().limit(5).forEach(post -> {
            String fragmento = post.selftext().isBlank() ? "[sin texto]"
                    : post.selftext().substring(0, Math.min(120, post.selftext().length())).replace("\n", " ");
            System.out.printf("%n[%s]%n  %s%n  → %s%n", post.title(), fragmento, post.url());
        });
        System.out.printf("%n Total posts recibidos: %d%n", posts.size());

        assertThat(posts).isNotEmpty();
    }
}
