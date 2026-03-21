package com.libraryagent.ingestion.extractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenLibraryClientTest {

    MockRestServiceServer server;
    RestClientOpenLibraryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientOpenLibraryClient(builder);
    }

    @Test
    void shouldReturnSpanishTitleWhenEditionExistsWithDifferentTitle() {
        // Given — search devuelve el work key; editions devuelve una edición en español
        String searchJson = """
                {
                  "numFound": 3,
                  "docs": [{
                    "key": "/works/OL12345W",
                    "title": "The Mists of Avalon",
                    "author_name": ["Marion Zimmer Bradley"]
                  }]
                }
                """;
        String editionsJson = """
                {
                  "entries": [
                    {
                      "title": "Las brumas de Avalon",
                      "languages": [{"key": "/languages/spa"}]
                    }
                  ]
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&language=spa&limit=1&fields=key,title,author_name",
                        "The Mists of Avalon"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://openlibrary.org/works/OL12345W/editions.json?limit=100"))
                .andRespond(withSuccess(editionsJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("The Mists of Avalon");

        // Then
        assertThat(result).isPresent();
        SpanishEdition edition = result.get();
        assertThat(edition.available()).isTrue();
        assertThat(edition.titleEs()).isEqualTo("Las brumas de Avalon");
        assertThat(edition.titleOriginal()).isEqualTo("The Mists of Avalon");
        assertThat(edition.author()).isEqualTo("Marion Zimmer Bradley");
    }

    @Test
    void shouldReturnNullTitleEsWhenSpanishEditionHasSameTitleAsEnglish() {
        // Given — Dune: la edición en español también se llama "Dune"
        String searchJson = """
                {
                  "numFound": 25,
                  "docs": [{
                    "key": "/works/OL00001W",
                    "title": "Dune",
                    "author_name": ["Frank Herbert"]
                  }]
                }
                """;
        String editionsJson = """
                {
                  "entries": [
                    {
                      "title": "Dune",
                      "languages": [{"key": "/languages/spa"}]
                    }
                  ]
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&language=spa&limit=1&fields=key,title,author_name",
                        "Dune"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://openlibrary.org/works/OL00001W/editions.json?limit=100"))
                .andRespond(withSuccess(editionsJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Dune");

        // Then
        assertThat(result).isPresent();
        SpanishEdition edition = result.get();
        assertThat(edition.available()).isTrue();
        assertThat(edition.titleEs()).isNull();
        assertThat(edition.author()).isEqualTo("Frank Herbert");
    }

    @Test
    void shouldReturnNotAvailableWhenNoSpanishEditionFound() {
        // Given — numFound 0: no hay works con edición en español
        String searchJson = """
                {
                  "numFound": 0,
                  "docs": []
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&language=spa&limit=1&fields=key,title,author_name",
                        "Some Unknown Book"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Some Unknown Book");

        // Then
        assertThat(result).isPresent();
        SpanishEdition edition = result.get();
        assertThat(edition.available()).isFalse();
        assertThat(edition.titleEs()).isNull();
        assertThat(edition.author()).isNull();
        assertThat(edition.titleOriginal()).isEqualTo("Some Unknown Book");
    }

    @Test
    void shouldReturnEmptyWhenApiReturnsServerError() {
        // Given
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&language=spa&limit=1&fields=key,title,author_name",
                        "Dune"))
                .andRespond(withServerError());

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Dune");

        // Then
        assertThat(result).isEmpty();
    }
}
