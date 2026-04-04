package com.libraryagent.ingestion.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CasaDelLibroScraperServiceImpl implements CasaDelLibroScraperService {

    private static final Logger log = LoggerFactory.getLogger(CasaDelLibroScraperServiceImpl.class);

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final String EXTRACT_PROMPT = """
            Analiza estos datos extraídos de una página de Casa del Libro y devuelve \
            SOLO un JSON con esta estructura exacta (sin markdown, sin explicaciones):
            {
              "coverUrl": "URL completa de la imagen de portada",
              "synopsis": "sinopsis completa del libro",
              "genres": ["género1", "género2"],
              "technicalSheet": {
                "Editorial": "...",
                "ISBN": "...",
                "Número de páginas": "...",
                "Año de edición": "YYYY",
                "Encuadernación": "...",
                "Idioma": "...",
                "Dimensiones": "...",
                "Peso": "..."
              }
            }

            Reglas:
            - coverUrl: usa image.contentUrl del JSON-LD Book si existe; si la URL contiene /t1/ o \
            /t5/ en el path, sustitúyelo por /s5/ y cambia la extensión a .webp; \
            si no hay JSON-LD usa og:image aplicando la misma transformación
            - synopsis: usa el texto de .resumen-content si está disponible; si no, \
            usa description del JSON-LD Book
            - genres: extrae del itemListElement del BreadcrumbList JSON-LD, \
            omite los elementos con name "Home" y "Libros"
            - technicalSheet: extrae de JSON-LD (workExample[0] para isbn/numberOfPages/\
            datePublished/bookFormat/inLanguage; publisher.name para Editorial; \
            size para Dimensiones; materialExtent para Peso); \
            para Año de edición usa solo los 4 primeros dígitos de datePublished; \
            para Encuadernación extrae la parte final de la URL de bookFormat
            - Omite de technicalSheet los campos sin valor
            - Si no hay datos para un campo raíz, usa null para strings, [] para arrays
            - Responde SOLO con el JSON

            Datos extraídos de la página:

            === JSON-LD ===
            %s

            === Texto de sinopsis (.resumen-content) ===
            %s

            === og:image ===
            %s
            """;

    private final ObjectMapper objectMapper;
    private final AnthropicClient anthropicClient;

    public CasaDelLibroScraperServiceImpl(
            ObjectMapper objectMapper,
            @Value("${anthropic.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.anthropicClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public CdlEnrichmentResultDto scrape(String url) {
        if (url == null || url.isBlank()) {
            throw new RuntimeException("La URL de Casa del Libro no puede estar vacía");
        }

        Document document;
        try {
            document = fetchDocument(url);
        } catch (IOException e) {
            throw new RuntimeException("Error al conectar con Casa del Libro: " + e.getMessage(), e);
        }

        String ldJson = document.select("script[type=application/ld+json]")
                .stream()
                .map(el -> el.data())
                .collect(Collectors.joining("\n---\n"));

        String synopsis = document.select(".resumen-content").text();
        String ogImage = document.select("meta[property=og:image]").attr("content");

        String prompt = EXTRACT_PROMPT.formatted(ldJson, synopsis, ogImage);
        String json = stripMarkdown(callClaude(prompt));
        return parseResponse(json);
    }

    // Extraído para permitir override en tests
    protected Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
    }

    // Extraído para permitir override en tests
    protected String callClaude(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5_20251001)
                .maxTokens(4096L)
                .addUserMessage(prompt)
                .build();

        return anthropicClient.messages().create(params)
                .content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Claude no devolvió respuesta"));
    }

    private String stripMarkdown(String response) {
        log.debug("Respuesta cruda de Claude: {}", response);
        String trimmed = response.trim();
        // Eliminar bloque de código markdown triple-backtick si existe
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        // Extraer entre el primer '{' y el último '}' para ignorar texto extra
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        log.warn("Claude no devolvió JSON parseable. Respuesta: {}", response);
        return trimmed;
    }

    private CdlEnrichmentResultDto parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String coverUrl = nullableText(root, "coverUrl");
            String synopsisText = nullableText(root, "synopsis");

            JsonNode sheetNode = root.path("technicalSheet");
            String technicalSheet = null;
            if (!sheetNode.isMissingNode() && !sheetNode.isNull() && sheetNode.size() > 0) {
                technicalSheet = objectMapper.writeValueAsString(sheetNode);
            }

            List<String> genres = new ArrayList<>();
            JsonNode genresNode = root.path("genres");
            if (genresNode.isArray()) {
                for (JsonNode g : genresNode) {
                    String name = g.asText();
                    if (!name.isBlank()) genres.add(name);
                }
            }

            return new CdlEnrichmentResultDto(coverUrl, synopsisText, technicalSheet, genres);
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Claude: " + e.getMessage() + " | JSON: " + json, e);
        }
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode child = node.path(field);
        if (child.isMissingNode() || child.isNull()) return null;
        String text = child.asText();
        return text.isBlank() ? null : text;
    }
}
