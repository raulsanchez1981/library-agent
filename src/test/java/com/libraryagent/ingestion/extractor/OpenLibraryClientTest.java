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
    void shouldReturnSpanishTitleWhenEditionExists() {
        // Given — OL encuentra el work y tiene una edición en español
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
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
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
    void shouldReturnSameTitleWhenSpanishEditionHasSameTitleAsEnglish() {
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
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
                        "Dune"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://openlibrary.org/works/OL00001W/editions.json?limit=100"))
                .andRespond(withSuccess(editionsJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Dune");

        // Then — disponible aunque el título sea idéntico al inglés
        assertThat(result).isPresent();
        assertThat(result.get().available()).isTrue();
        assertThat(result.get().titleEs()).isEqualTo("Dune");
        assertThat(result.get().author()).isEqualTo("Frank Herbert");
    }

    @Test
    void shouldReturnEmptyWhenNoWorkFoundInOL() {
        // Given — OL no tiene el libro
        String searchJson = """
                {
                  "numFound": 0,
                  "docs": []
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
                        "Some Unknown Book"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Some Unknown Book");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenWorkFoundButNoSpanishEdition() {
        // Given — OL tiene el work pero ninguna edición en español
        String searchJson = """
                {
                  "numFound": 1,
                  "docs": [{
                    "key": "/works/OL99999W",
                    "title": "Gridlinked",
                    "author_name": ["Neal Asher"]
                  }]
                }
                """;
        String editionsJson = """
                {
                  "entries": [
                    {
                      "title": "Gridlinked",
                      "languages": [{"key": "/languages/eng"}]
                    }
                  ]
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
                        "Gridlinked"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://openlibrary.org/works/OL99999W/editions.json?limit=100"))
                .andRespond(withSuccess(editionsJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Gridlinked");

        // Then — libro en OL pero sin edición española
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectOLResultWhenMultiWordTitleSimilarityIsTooLow() {
        // Given — OL devuelve un libro con título completamente diferente (falso positivo)
        String searchJson = """
                {
                  "numFound": 1,
                  "docs": [{
                    "key": "/works/OL11111W",
                    "title": "Diccionario Del Diablo",
                    "author_name": ["Ambrose Bierce"]
                  }]
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
                        "The Devils"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("The Devils");

        // Then — rechazado por baja similitud de título
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectOLResultWhenSingleWordTitleMatchesLongerTitle() {
        // Given — buscar "Cradle" no debe aceptar "Cat's Cradle" (Vonnegut)
        String searchJson = """
                {
                  "numFound": 1,
                  "docs": [{
                    "key": "/works/OL22222W",
                    "title": "Cat's Cradle",
                    "author_name": ["Kurt Vonnegut"]
                  }]
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
                        "Cradle"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Cradle");

        // Then — "Cradle" ≠ "Cat's Cradle": coincidencia exacta requerida para título de una sola palabra
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenApiReturnsServerError() {
        // Given
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&limit=5&fields=key,title,author_name,cover_i",
                        "Dune"))
                .andRespond(withServerError());

        // When
        Optional<SpanishEdition> result = client.findSpanishEdition("Dune");

        // Then
        assertThat(result).isEmpty();
    }

    // ── findBySpanishTitle ────────────────────────────────────────────────────

    @Test
    void shouldReturnSpanishEditionWhenOLRecognizesSpanishTitle() {
        // Given
        String searchJson = """
                {
                  "numFound": 1,
                  "docs": [{
                    "key": "/works/OL12345W",
                    "title": "El Señor de los Anillos",
                    "author_name": ["J.R.R. Tolkien"]
                  }]
                }
                """;
        String editionsJson = """
                {
                  "entries": [
                    {
                      "title": "El Señor de los Anillos",
                      "languages": [{"key": "/languages/spa"}]
                    }
                  ]
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&language=spa&limit=5&fields=key,title,author_name,cover_i",
                        "El Señor de los Anillos"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://openlibrary.org/works/OL12345W/editions.json?limit=100"))
                .andRespond(withSuccess(editionsJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findBySpanishTitle("El Señor de los Anillos");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().titleEs()).isEqualTo("El Señor de los Anillos");
        assertThat(result.get().author()).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    void shouldReturnEmptyWhenOLDoesNotRecognizeSpanishTitle() {
        // Given — OL no tiene nada para este título español
        String searchJson = """
                {
                  "numFound": 0,
                  "docs": []
                }
                """;
        server.expect(requestToUriTemplate(
                        "https://openlibrary.org/search.json?title={t}&language=spa&limit=5&fields=key,title,author_name,cover_i",
                        "Los magos"))
                .andRespond(withSuccess(searchJson, MediaType.APPLICATION_JSON));

        // When
        Optional<SpanishEdition> result = client.findBySpanishTitle("Los magos");

        // Then
        assertThat(result).isEmpty();
    }
}
