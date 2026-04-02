package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CasaDelLibroScraperServiceTest {

    private static final String SAMPLE_HTML = """
            <html>
            <head>
              <meta property="og:image" content="https://img.casadellibro.com/portada.jpg"/>
            </head>
            <body>
              <div class="description-text">Una novela épica sobre la búsqueda del conocimiento.</div>
              <div class="book-details-tags">
                <a href="#">Fantasía</a>
                <a href="#">Aventura</a>
              </div>
              <dl class="book-details">
                <dt>ISBN</dt><dd>978-84-12345-67-8</dd>
                <dt>Editorial</dt><dd>Minotauro</dd>
                <dt>Páginas</dt><dd>450</dd>
              </dl>
            </body>
            </html>
            """;

    private static final String EMPTY_HTML = """
            <html><head></head><body></body></html>
            """;

    private CasaDelLibroScraperServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CasaDelLibroScraperServiceImpl(new ObjectMapper()) {
            @Override
            protected Document fetchDocument(String url) {
                return Jsoup.parse(SAMPLE_HTML, url);
            }
        };
    }

    @Test
    void shouldExtractCoverUrlFromOgImageMeta() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.coverUrl()).isEqualTo("https://img.casadellibro.com/portada.jpg");
    }

    @Test
    void shouldExtractSynopsisFromDescriptionText() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.synopsis()).isEqualTo("Una novela épica sobre la búsqueda del conocimiento.");
    }

    @Test
    void shouldExtractGenresFromTagLinks() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.genres()).containsExactly("Fantasía", "Aventura");
    }

    @Test
    void shouldExtractTechnicalSheetAsJsonFromDtDdPairs() throws Exception {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.technicalSheet()).isNotNull();
        ObjectMapper mapper = new ObjectMapper();
        var sheet = mapper.readValue(result.technicalSheet(), java.util.Map.class);
        assertThat(sheet).containsEntry("ISBN", "978-84-12345-67-8");
        assertThat(sheet).containsEntry("Editorial", "Minotauro");
        assertThat(sheet).containsEntry("Páginas", "450");
    }

    @Test
    void shouldReturnNullFieldsWhenPageHasNoRelevantContent() {
        // Given
        CasaDelLibroScraperServiceImpl emptyService = new CasaDelLibroScraperServiceImpl(new ObjectMapper()) {
            @Override
            protected Document fetchDocument(String url) {
                return Jsoup.parse(EMPTY_HTML, url);
            }
        };

        // When
        CdlEnrichmentResultDto result = emptyService.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.coverUrl()).isNull();
        assertThat(result.synopsis()).isNull();
        assertThat(result.technicalSheet()).isNull();
        assertThat(result.genres()).isEmpty();
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUrlIsBlank() {
        // When / Then
        assertThatThrownBy(() -> service.scrape(""))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URL de Casa del Libro no puede estar vacía");
    }

    @Test
    void shouldThrowRuntimeExceptionWhenConnectionFails() {
        // Given
        CasaDelLibroScraperServiceImpl failingService = new CasaDelLibroScraperServiceImpl(new ObjectMapper()) {
            @Override
            protected Document fetchDocument(String url) throws IOException {
                throw new IOException("Connection refused");
            }
        };

        // When / Then
        assertThatThrownBy(() -> failingService.scrape("https://www.casadellibro.com/libro"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al conectar con Casa del Libro");
    }
}
