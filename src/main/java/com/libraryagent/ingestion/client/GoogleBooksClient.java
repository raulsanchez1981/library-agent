package com.libraryagent.ingestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.libraryagent.ingestion.dto.GoogleBooksEnrichmentDto;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class GoogleBooksClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);
    private static final String BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoogleBooksClient(
            ObjectMapper objectMapper,
            @Value("${google.books.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<GoogleBooksResult> search(String title, String author) {
        return search(title, author, null);
    }

    /**
     * @param title         Título principal (preferiblemente en español)
     * @param author        Autor
     * @param originalTitle Título original en inglés como fallback (puede ser null)
     */
    public Optional<GoogleBooksResult> search(String title, String author, String originalTitle) {
        // 1. Título español + autor + langRestrict=es
        Optional<GoogleBooksResult> result = doSearch(title, author, "es");
        if (result.isPresent()) return result;

        // 2. Título español + autor (sin restricción de idioma)
        result = doSearch(title, author, null);
        if (result.isPresent()) return result;

        // 3. Título original en inglés + autor
        if (originalTitle != null && !originalTitle.equalsIgnoreCase(title)) {
            result = doSearch(originalTitle, author, null);
            if (result.isPresent()) return result;
        }

        // 4. Solo título español, sin autor (más permisivo)
        result = doSearch(title, null, "es");
        if (result.isPresent()) return result;

        return doSearch(title, null, null);
    }

    /**
     * Busca el volumen más adecuado en Google Books para enriquecer un título.
     * Prioriza: coincidencia de título + ISBN 9788.
     * Fallback: coincidencia de título + language=es.
     */
    public Optional<GoogleBooksEnrichmentDto> findBestSpanishVolume(String titleEs, String author) {
        try {
            String encodedTitle = URLEncoder.encode(titleEs != null ? titleEs : "", StandardCharsets.UTF_8);
            String query = "intitle:" + encodedTitle;
            if (author != null && !author.isBlank()) {
                query += "+inauthor:" + URLEncoder.encode(author, StandardCharsets.UTF_8);
            }
            String url = BASE_URL + "?q=" + query
                    + "&maxResults=10"
                    + "&langRestrict=es"
                    + "&key=" + apiKey;

            log.debug("Google Books findBestSpanishVolume URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Google Books findBestSpanishVolume: status {} para '{}'", response.statusCode(), titleEs);
            if (response.statusCode() != 200) return Optional.empty();

            JsonNode items = objectMapper.readTree(response.body()).path("items");
            if (!items.isArray() || items.isEmpty()) return Optional.empty();

            // Paso 1: título + language=es + synopsis
            // Google ordena por relevancia: el primer resultado con datos completos es el mejor.
            // Nota: langRestrict=es ya filtra la respuesta, así que todos los items son en español.
            for (JsonNode item : items) {
                JsonNode vi = item.path("volumeInfo");
                if (titlesMatch(titleEs, vi.path("title").asText(""))
                        && "es".equals(vi.path("language").asText(""))
                        && !vi.path("description").isMissingNode()
                        && !vi.path("description").asText("").isBlank()) {
                    String isbn = extractIsbn(vi);
                    log.info("Google Books: volumen en español con synopsis para '{}'", titleEs);
                    return Optional.of(buildEnrichmentDto(item.path("id").asText(null), vi, isbn));
                }
            }

            // Paso 2: título + ISBN 9788 + synopsis (edición peninsular con datos)
            for (JsonNode item : items) {
                JsonNode vi = item.path("volumeInfo");
                String isbn = extractIsbn(vi);
                if (titlesMatch(titleEs, vi.path("title").asText(""))
                        && isbn != null && isbn.startsWith("9788")
                        && !vi.path("description").isMissingNode()
                        && !vi.path("description").asText("").isBlank()) {
                    log.info("Google Books: volumen con ISBN 9788 y synopsis para '{}'", titleEs);
                    return Optional.of(buildEnrichmentDto(item.path("id").asText(null), vi, isbn));
                }
            }

            // Paso 3: título + language=es sin synopsis (al menos en español)
            for (JsonNode item : items) {
                JsonNode vi = item.path("volumeInfo");
                if (titlesMatch(titleEs, vi.path("title").asText(""))
                        && "es".equals(vi.path("language").asText(""))) {
                    String isbn = extractIsbn(vi);
                    log.info("Google Books: volumen en español (sin synopsis) para '{}'", titleEs);
                    return Optional.of(buildEnrichmentDto(item.path("id").asText(null), vi, isbn));
                }
            }

            // Paso 4: título + ISBN 9788 sin synopsis (último recurso)
            for (JsonNode item : items) {
                JsonNode vi = item.path("volumeInfo");
                String isbn = extractIsbn(vi);
                if (titlesMatch(titleEs, vi.path("title").asText(""))
                        && isbn != null && isbn.startsWith("9788")) {
                    log.info("Google Books: volumen con ISBN 9788 (sin synopsis) para '{}'", titleEs);
                    return Optional.of(buildEnrichmentDto(item.path("id").asText(null), vi, isbn));
                }
            }

            log.debug("Google Books: ningún volumen adecuado encontrado para '{}'", titleEs);
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Error en findBestSpanishVolume para '{}': {}", titleEs, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean titlesMatch(String searched, String result) {
        String normSearched = normalizeTitle(searched);
        String normResult = normalizeTitle(result);
        if (normResult.contains(normSearched) || normSearched.contains(normResult)) return true;

        String[] words = normSearched.split("\\s+");
        long significant = Arrays.stream(words).filter(w -> w.length() >= 4).count();
        if (significant == 0) return true;
        long matched = Arrays.stream(words)
                .filter(w -> w.length() >= 4 && normResult.contains(w))
                .count();
        return (double) matched / significant >= 0.6;
    }

    private String normalizeTitle(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private GoogleBooksEnrichmentDto buildEnrichmentDto(String googleBooksId, JsonNode vi, String isbn) {
        return new GoogleBooksEnrichmentDto(
                googleBooksId,
                findBestCoverUrl(vi, isbn),
                vi.path("description").asText(null),
                vi.path("publisher").asText(null),
                vi.path("publishedDate").asText(null),
                vi.path("pageCount").isInt() ? vi.path("pageCount").intValue() : null,
                isbn
        );
    }

    /**
     * Devuelve la URL de portada de Google Books (thumbnail limpio).
     * No valida el contenido — acepta lo que Google sirva.
     * Si no hay imageLinks en la respuesta, devuelve null.
     */
    private String findBestCoverUrl(JsonNode volumeInfo, String isbn) {
        JsonNode imageLinks = volumeInfo.path("imageLinks");
        if (imageLinks.isMissingNode()) return null;
        String thumbnail = imageLinks.path("thumbnail").asText(null);
        if (thumbnail == null) return null;
        thumbnail = thumbnail.replace("http://", "https://");
        thumbnail = thumbnail.replaceAll("&edge=curl", "");
        return thumbnail;
    }

    /**
     * Busca el ISBN de edición española (prefijo 978-84) para un título dado.
     * Hace una única llamada con langRestrict=es y maxResults=5, iterando los
     * resultados hasta encontrar un ISBN que empiece por "9788".
     * Devuelve Optional.empty() si ningún resultado tiene ISBN español.
     */
    public Optional<String> findSpanishIsbn(String titleEs, String author) {
        try {
            String encodedTitle = URLEncoder.encode(titleEs != null ? titleEs : "", StandardCharsets.UTF_8);
            String query = "intitle:" + encodedTitle;
            if (author != null && !author.isBlank()) {
                query += "+inauthor:" + URLEncoder.encode(author, StandardCharsets.UTF_8);
            }
            String url = BASE_URL + "?q=" + query
                    + "&maxResults=5"
                    + "&langRestrict=es"
                    + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Google Books ISBN search URL: {}", url);
            log.info("Google Books ISBN search: status {} para '{}'", response.statusCode(), titleEs);
            if (response.statusCode() != 200) return Optional.empty();

            JsonNode items = objectMapper.readTree(response.body()).path("items");
            if (!items.isArray() || items.isEmpty()) return Optional.empty();

            for (JsonNode item : items) {
                String isbn = extractIsbn(item.path("volumeInfo"));
                if (isbn != null && isbn.startsWith("9788")) {
                    log.info("ISBN español encontrado para '{}': {}", titleEs, isbn);
                    return Optional.of(isbn);
                }
            }

            log.debug("Ningún resultado con ISBN 978-84 para '{}' ({} resultados revisados)", titleEs, items.size());
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Error al buscar ISBN español para '{}': {}", titleEs, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GoogleBooksResult> doSearch(String title, String author, String langRestrict) {
        try {
            String encodedTitle = URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8);
            String query = "intitle:" + encodedTitle;
            if (author != null && !author.isBlank()) {
                query += "+inauthor:" + URLEncoder.encode(author, StandardCharsets.UTF_8);
            }
            String url = BASE_URL + "?q=" + query
                    + "&maxResults=1"
                    + (langRestrict != null ? "&langRestrict=" + langRestrict : "")
                    + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Google Books API status {} para título '{}'", response.statusCode(), title);
            if (response.statusCode() != 200) {
                log.warn("Google Books API respondió con status {} — body: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            log.warn("Error al consultar Google Books para título '{}': {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GoogleBooksResult> parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return Optional.empty();
            }

            JsonNode item = items.get(0);
            String googleBooksId = item.path("id").asText(null);
            JsonNode volumeInfo = item.path("volumeInfo");

            String synopsis = volumeInfo.path("description").asText(null);
            String coverUrl = extractCoverUrl(volumeInfo);
            String isbn = extractIsbn(volumeInfo);

            log.info("Google Books resultado — id: {}, isbn: {}, cover: {}, synopsis: {}", googleBooksId, isbn, coverUrl, synopsis != null ? "sí" : "no");
            return Optional.of(new GoogleBooksResult(googleBooksId, coverUrl, synopsis, isbn));

        } catch (Exception e) {
            log.warn("Error al parsear respuesta de Google Books: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractCoverUrl(JsonNode volumeInfo) {
        JsonNode imageLinks = volumeInfo.path("imageLinks");
        if (imageLinks.isMissingNode()) {
            return null;
        }
        String thumbnail = imageLinks.path("thumbnail").asText(null);
        if (thumbnail == null) {
            return null;
        }
        // Forzar HTTPS, eliminar edge=curl y aumentar resolución
        thumbnail = thumbnail.replace("http://", "https://");
        thumbnail = thumbnail.replaceAll("&edge=curl", "");
        thumbnail = thumbnail.replaceAll("&zoom=\\d+", "&zoom=3");
        if (!thumbnail.contains("zoom=")) thumbnail += "&zoom=3";
        thumbnail += "&fife=w400";
        return thumbnail;
    }

    private String extractIsbn(JsonNode volumeInfo) {
        JsonNode identifiers = volumeInfo.path("industryIdentifiers");
        if (!identifiers.isArray()) return null;

        String isbn10 = null;
        for (JsonNode id : identifiers) {
            String type = id.path("type").asText("");
            String value = id.path("identifier").asText(null);
            if ("ISBN_13".equals(type)) return value;
            if ("ISBN_10".equals(type)) isbn10 = value;
        }
        return isbn10;
    }

    public record GoogleBooksResult(String googleBooksId, String coverUrl, String synopsis, String isbn) {}
}
