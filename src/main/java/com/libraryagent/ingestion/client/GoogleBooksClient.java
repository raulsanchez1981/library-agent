package com.libraryagent.ingestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

            log.info("Google Books resultado — id: {}, cover: {}, synopsis: {}", googleBooksId, coverUrl, synopsis != null ? "sí" : "no");
            return Optional.of(new GoogleBooksResult(googleBooksId, coverUrl, synopsis));

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

    public record GoogleBooksResult(String googleBooksId, String coverUrl, String synopsis) {}
}
