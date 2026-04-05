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

    // Respuesta típica que devolvería Claude Haiku
    private static final String CLAUDE_RESPONSE = """
            {
              "coverUrl": "https://imagessl7.casadellibro.com/a/l/s5/87/9788413143187.webp",
              "synopsis": "Una novela épica sobre la búsqueda del conocimiento.",
              "genres": ["Literatura", "Narrativa en bolsillo", "Fantástica en bolsillo"],
              "technicalSheet": {
                "Editorial": "B de Bolsillo",
                "ISBN": "9788413143187",
                "Número de páginas": "2224",
                "Año de edición": "2021",
                "Encuadernación": "Paperback",
                "Idioma": "es",
                "Dimensiones": "19.2 x 12.8 x 13.4 cm",
                "Peso": "1548.0g"
              }
            }
            """;

    private static final String SAMPLE_HTML = """
            <html>
            <head>
              <meta property="og:image" content="https://imagessl7.casadellibro.com/a/l/t1/87/9788413143187.jpg"/>
              <script type="application/ld+json">{"@type":"Book","name":"Nacidos de la Bruma"}</script>
            </head>
            <body>
              <div class="resumen-content svelte-g9q8l2">Una novela épica sobre la búsqueda del conocimiento.</div>
            </body>
            </html>
            """;

    private CasaDelLibroScraperServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CasaDelLibroScraperServiceImpl(new ObjectMapper(), null) {
            @Override
            protected Document fetchDocument(String url) {
                return Jsoup.parse(SAMPLE_HTML, url);
            }

            @Override
            protected String callClaude(String prompt) {
                return CLAUDE_RESPONSE;
            }
        };
    }

    @Test
    void shouldReturnCoverUrlFromClaudeResponse() {
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        assertThat(result.coverUrl()).isEqualTo("https://imagessl7.casadellibro.com/a/l/s5/87/9788413143187.webp");
    }

    @Test
    void shouldReturnSynopsisFromClaudeResponse() {
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        assertThat(result.synopsis()).isEqualTo("Una novela épica sobre la búsqueda del conocimiento.");
    }

    @Test
    void shouldReturnGenresFromClaudeResponse() {
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        assertThat(result.genres()).containsExactly("Literatura", "Narrativa en bolsillo", "Fantástica en bolsillo");
    }

    @Test
    void shouldReturnTechnicalSheetFromClaudeResponse() throws Exception {
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        assertThat(result.technicalSheet()).isNotNull();
        var sheet = new ObjectMapper().readValue(result.technicalSheet(), java.util.Map.class);
        assertThat(sheet).containsEntry("Editorial", "B de Bolsillo");
        assertThat(sheet).containsEntry("ISBN", "9788413143187");
        assertThat(sheet).containsEntry("Número de páginas", "2224");
        assertThat(sheet).containsEntry("Año de edición", "2021");
    }

    @Test
    void shouldReturnNullFieldsWhenClaudeReturnsNulls() {
        // Given — Claude devuelve nulls/arrays vacíos
        CasaDelLibroScraperServiceImpl emptyService = new CasaDelLibroScraperServiceImpl(new ObjectMapper(), null) {
            @Override
            protected Document fetchDocument(String url) {
                return Jsoup.parse("<html><head></head><body></body></html>", url);
            }

            @Override
            protected String callClaude(String prompt) {
                return """
                        {"coverUrl": null, "synopsis": null, "genres": [], "technicalSheet": {}}
                        """;
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
        assertThatThrownBy(() -> service.scrape(""))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("URL de Casa del Libro no puede estar vacía");
    }

    @Test
    void shouldThrowRuntimeExceptionWhenConnectionFails() {
        // Given
        CasaDelLibroScraperServiceImpl failingService = new CasaDelLibroScraperServiceImpl(new ObjectMapper(), null) {
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

    @Test
    void shouldThrowRuntimeExceptionWhenClaudeReturnsMalformedJson() {
        // Given
        CasaDelLibroScraperServiceImpl badJsonService = new CasaDelLibroScraperServiceImpl(new ObjectMapper(), null) {
            @Override
            protected Document fetchDocument(String url) {
                return Jsoup.parse(SAMPLE_HTML, url);
            }

            @Override
            protected String callClaude(String prompt) {
                return "esto no es JSON";
            }
        };

        // When / Then
        assertThatThrownBy(() -> badJsonService.scrape("https://www.casadellibro.com/libro"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al parsear respuesta de Claude");
    }
}
