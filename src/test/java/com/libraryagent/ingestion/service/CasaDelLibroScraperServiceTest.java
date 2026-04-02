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

    // HTML representativo de la estructura real de CDL (SSR Svelte + JSON-LD)
    private static final String SAMPLE_HTML = """
            <html>
            <head>
              <meta property="og:image" content="https://imagessl7.casadellibro.com/a/l/t1/87/9788413143187.jpg"/>
              <script type="application/ld+json">
              {
                "@context": "http://schema.org/",
                "@type": "Book",
                "image": {
                  "@type": "ImageObject",
                  "contentUrl": "https://imagessl7.casadellibro.com/a/l/t5/87/9788413143187.jpg",
                  "thumbnailUrl": "https://imagessl7.casadellibro.com/a/l/t1/87/9788413143187.jpg"
                },
                "name": "Nacidos de la Bruma",
                "description": "Sinopsis corta del libro.",
                "publisher": { "@type": "Organization", "name": "B de Bolsillo" },
                "size": "19.2 x 12.8 x 13.4 cm",
                "materialExtent": "1548.0g",
                "workExample": [{
                  "@type": ["Product", "Book"],
                  "isbn": "9788413143187",
                  "datePublished": "2021-04-15",
                  "bookFormat": "https://schema.org/Paperback",
                  "numberOfPages": 2224,
                  "inLanguage": "es"
                }]
              }
              </script>
              <script type="application/ld+json">
              {
                "@context": "http://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": [
                  { "@type": "ListItem", "position": 0, "name": "Home" },
                  { "@type": "ListItem", "position": 1, "name": "Libros" },
                  { "@type": "ListItem", "position": 2, "name": "Literatura" },
                  { "@type": "ListItem", "position": 3, "name": "Narrativa en bolsillo" },
                  { "@type": "ListItem", "position": 4, "name": "Fantástica en bolsillo" }
                ]
              }
              </script>
            </head>
            <body>
              <div class="resumen svelte-g9q8l2">
                <div class="resumen-content svelte-g9q8l2">Una novela épica sobre la búsqueda del conocimiento.</div>
              </div>
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
    void shouldExtractCoverUrlFromJsonLdContentUrl() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then — prefiere JSON-LD image.contentUrl (t5) sobre og:image (t1)
        assertThat(result.coverUrl()).isEqualTo("https://imagessl7.casadellibro.com/a/l/t5/87/9788413143187.jpg");
    }

    @Test
    void shouldExtractSynopsisFromResumenContent() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.synopsis()).isEqualTo("Una novela épica sobre la búsqueda del conocimiento.");
    }

    @Test
    void shouldExtractGenresFromBreadcrumbListSkippingHomeAndLibros() {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then — posiciones 0 (Home) y 1 (Libros) se omiten
        assertThat(result.genres()).containsExactly("Literatura", "Narrativa en bolsillo", "Fantástica en bolsillo");
    }

    @Test
    void shouldExtractTechnicalSheetFromJsonLd() throws Exception {
        // When
        CdlEnrichmentResultDto result = service.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.technicalSheet()).isNotNull();
        var sheet = new ObjectMapper().readValue(result.technicalSheet(), java.util.Map.class);
        assertThat(sheet).containsEntry("Editorial", "B de Bolsillo");
        assertThat(sheet).containsEntry("ISBN", "9788413143187");
        assertThat(sheet).containsEntry("Número de páginas", "2224");
        assertThat(sheet).containsEntry("Año de edición", "2021");
        assertThat(sheet).containsEntry("Encuadernación", "Paperback");
        assertThat(sheet).containsEntry("Idioma", "es");
        assertThat(sheet).containsEntry("Dimensiones", "19.2 x 12.8 x 13.4 cm");
        assertThat(sheet).containsEntry("Peso", "1548.0g");
    }

    @Test
    void shouldFallbackToOgImageWhenJsonLdHasNoImage() {
        // Given — JSON-LD sin campo image
        CasaDelLibroScraperServiceImpl noImageService = new CasaDelLibroScraperServiceImpl(new ObjectMapper()) {
            @Override
            protected Document fetchDocument(String url) {
                return Jsoup.parse("""
                        <html>
                        <head>
                          <meta property="og:image" content="https://imagessl7.casadellibro.com/a/l/t1/87/fallback.jpg"/>
                          <script type="application/ld+json">{"@type":"Book","name":"Test"}</script>
                        </head><body></body></html>
                        """, url);
            }
        };

        // When
        CdlEnrichmentResultDto result = noImageService.scrape("https://www.casadellibro.com/libro");

        // Then
        assertThat(result.coverUrl()).isEqualTo("https://imagessl7.casadellibro.com/a/l/t1/87/fallback.jpg");
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
