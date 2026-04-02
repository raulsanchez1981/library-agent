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
              <meta property="og:image" content="https://imagessl7.casadellibro.com/a/l/t1/87/9788413143187.jpg"/>
            </head>
            <body>
              <div class="portada svelte-tu5qay">
                <cdl-img class="svelte-k5yotb">
                  <img class="svelte-k5yotb"
                       src="https://imagessl7.casadellibro.com/a/l/s5/87/9788413143187.webp"
                       alt="portada del libro"/>
                </cdl-img>
              </div>
              <div class="resumen svelte-g9q8l2">
                <div class="resumen-content svelte-g9q8l2">Una novela épica sobre la búsqueda del conocimiento.</div>
              </div>
              <span class="genero svelte-my94d2">Fantasía</span>
              <span class="genero svelte-my94d2">Aventura</span>
              <dl class="campo svelte-1yhk452">
                <dt>ISBN:</dt><dd>978-84-12345-67-8</dd>
                <dt>Editorial:</dt><dd>Minotauro</dd>
                <dt>Número de páginas:</dt><dd>450</dd>
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
    void shouldExtractCoverUrlFromPortadaImg() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then — prefiere .portada img (s5, mayor resolución) sobre og:image (t1)
        assertThat(result.coverUrl()).isEqualTo("https://imagessl7.casadellibro.com/a/l/s5/87/9788413143187.webp");
    }

    @Test
    void shouldExtractSynopsisFromResumenContent() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.synopsis()).isEqualTo("Una novela épica sobre la búsqueda del conocimiento.");
    }

    @Test
    void shouldExtractGenresFromGeneroSpans() {
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
        assertThat(sheet).containsEntry("Número de páginas", "450");
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
