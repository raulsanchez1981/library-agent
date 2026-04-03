package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CasaDelLibroScraperServiceImpl implements CasaDelLibroScraperService {

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final ObjectMapper objectMapper;

    public CasaDelLibroScraperServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        // Parsear todos los bloques JSON-LD presentes en el HTML estático
        List<JsonNode> ldJsonBlocks = parseLdJsonBlocks(document);

        JsonNode bookNode = findByType(ldJsonBlocks, "Book");
        JsonNode breadcrumbNode = findByType(ldJsonBlocks, "BreadcrumbList");

        String coverUrl = extractCoverUrl(bookNode, document);
        String synopsis = extractSynopsis(document);
        List<String> genres = extractGenres(breadcrumbNode);
        String technicalSheet = extractTechnicalSheet(bookNode);

        return new CdlEnrichmentResultDto(coverUrl, synopsis, technicalSheet, genres);
    }

    // Extraído para permitir override en tests
    protected Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
    }

    private List<JsonNode> parseLdJsonBlocks(Document document) {
        List<JsonNode> blocks = new ArrayList<>();
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                blocks.add(objectMapper.readTree(script.data()));
            } catch (JsonProcessingException ignored) {
                // bloque JSON malformado — se ignora
            }
        }
        return blocks;
    }

    private JsonNode findByType(List<JsonNode> blocks, String type) {
        return blocks.stream()
                .filter(node -> {
                    JsonNode typeNode = node.path("@type");
                    if (typeNode.isArray()) {
                        for (JsonNode t : typeNode) {
                            if (type.equals(t.asText())) return true;
                        }
                        return false;
                    }
                    return type.equals(typeNode.asText());
                })
                .findFirst()
                .orElse(null);
    }

    private String extractCoverUrl(JsonNode bookNode, Document document) {
        // JSON-LD: image.contentUrl contiene t5; lo promovemos a s5 (mayor resolución)
        if (bookNode != null) {
            String contentUrl = bookNode.path("image").path("contentUrl").asText();
            if (!contentUrl.isBlank()) {
                return upgradeImageQuality(contentUrl);
            }
        }

        // Fallback: og:image (t1), también promovido a s5
        String ogImage = document.select("meta[property=og:image]").attr("content");
        return ogImage.isBlank() ? null : upgradeImageQuality(ogImage);
    }

    /**
     * CDL usa /a/l/{quality}/ en sus URLs de imagen.
     * t1/t5 son miniaturas JPEG; s5 es la versión estándar en WebP (mayor resolución).
     */
    private String upgradeImageQuality(String url) {
        if (url == null) return null;
        String upgraded = url.replace("/a/l/t1/", "/a/l/s5/")
                             .replace("/a/l/t5/", "/a/l/s5/");
        // s5 solo existe en .webp, no en .jpg
        if (upgraded.contains("/a/l/s5/") && upgraded.endsWith(".jpg")) {
            upgraded = upgraded.substring(0, upgraded.length() - 4) + ".webp";
        }
        return upgraded;
    }

    private String extractSynopsis(Document document) {
        // .resumen-content está en el HTML estático (SSR de Svelte)
        String text = document.select(".resumen-content").text();
        return text.isBlank() ? null : text;
    }

    private List<String> extractGenres(JsonNode breadcrumbNode) {
        // Los géneros están en el BreadcrumbList a partir de la posición 2
        // (0=Home, 1=Libros son demasiado genéricos)
        if (breadcrumbNode == null) return List.of();

        List<String> genres = new ArrayList<>();
        JsonNode items = breadcrumbNode.path("itemListElement");
        for (JsonNode item : items) {
            int position = item.path("position").asInt(-1);
            if (position >= 2) {
                String name = item.path("name").asText();
                if (!name.isBlank()) {
                    genres.add(name);
                }
            }
        }
        return genres;
    }

    private String extractTechnicalSheet(JsonNode bookNode) {
        if (bookNode == null) return null;

        Map<String, String> sheet = new LinkedHashMap<>();

        // Datos del publisher
        String editorial = bookNode.path("publisher").path("name").asText();
        if (!editorial.isBlank()) sheet.put("Editorial", editorial);

        // Datos del workExample (primer ejemplar con ISBN, páginas, fecha, formato)
        JsonNode examples = bookNode.path("workExample");
        JsonNode example = examples.isArray() && examples.size() > 0 ? examples.get(0) : examples;

        String isbn = example.path("isbn").asText();
        if (!isbn.isBlank()) sheet.put("ISBN", isbn);

        String pages = example.path("numberOfPages").asText();
        if (!pages.isBlank() && !"0".equals(pages)) sheet.put("Número de páginas", pages);

        String date = example.path("datePublished").asText();
        if (!date.isBlank()) sheet.put("Año de edición", date.length() >= 4 ? date.substring(0, 4) : date);

        String format = example.path("bookFormat").asText();
        if (!format.isBlank()) {
            // https://schema.org/Paperback → "Paperback"
            String formatName = format.contains("/") ? format.substring(format.lastIndexOf('/') + 1) : format;
            sheet.put("Encuadernación", formatName);
        }

        String language = example.path("inLanguage").asText();
        if (!language.isBlank()) sheet.put("Idioma", language);

        // Dimensiones y peso a nivel de libro
        String size = bookNode.path("size").asText();
        if (!size.isBlank()) sheet.put("Dimensiones", size);

        String weight = bookNode.path("materialExtent").asText();
        if (!weight.isBlank()) sheet.put("Peso", weight);

        if (sheet.isEmpty()) return null;

        try {
            return objectMapper.writeValueAsString(sheet);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar la ficha técnica a JSON", e);
        }
    }
}
