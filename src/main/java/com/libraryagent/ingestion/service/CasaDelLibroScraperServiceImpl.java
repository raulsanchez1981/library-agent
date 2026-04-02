package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

        String coverUrl = extractCoverUrl(document);
        String synopsis = extractSynopsis(document);
        List<String> genres = extractGenres(document);
        String technicalSheet = extractTechnicalSheet(document);

        return new CdlEnrichmentResultDto(coverUrl, synopsis, technicalSheet, genres);
    }

    // Método extraído para permitir override en tests
    protected Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
    }

    private String extractCoverUrl(Document document) {
        // og:image es la opción más fiable
        String ogImage = document.select("meta[property=og:image]").attr("content");
        if (!ogImage.isBlank()) {
            return ogImage;
        }

        // Fallback: img con clase que contenga "cover" o "portada"
        Element coverImg = document.selectFirst("img[class*=cover], img[class*=portada]");
        if (coverImg != null) {
            String src = coverImg.attr("abs:src");
            if (!src.isBlank()) {
                return src;
            }
        }

        return null;
    }

    private String extractSynopsis(Document document) {
        String text = document.select(
                ".description-text, .synopsis, [itemprop=description], " +
                ".book-description, .book-synopsis, .product-description"
        ).text();
        return text.isBlank() ? null : text;
    }

    private List<String> extractGenres(Document document) {
        Elements genreElements = document.select(
                ".book-details-tags a, .category a, [itemprop=genre], " +
                ".book-categories a, .genres a, .tags a"
        );

        return genreElements.stream()
                .map(Element::text)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String extractTechnicalSheet(Document document) {
        Map<String, String> sheet = new LinkedHashMap<>();

        // Patrón dt/dd
        Elements dts = document.select(".book-details dt, .technical-data dt, dl dt");
        for (Element dt : dts) {
            Element dd = dt.nextElementSibling();
            if (dd != null && dd.tagName().equals("dd")) {
                String key = dt.text().trim().replaceAll(":$", "");
                String value = dd.text().trim();
                if (!key.isBlank() && !value.isBlank()) {
                    sheet.put(key, value);
                }
            }
        }

        // Patrón label/value spans
        if (sheet.isEmpty()) {
            Elements labels = document.select("span.label, .book-data-label, .detail-label");
            for (Element label : labels) {
                Element value = label.nextElementSibling();
                if (value != null) {
                    String key = label.text().trim().replaceAll(":$", "");
                    String val = value.text().trim();
                    if (!key.isBlank() && !val.isBlank()) {
                        sheet.put(key, val);
                    }
                }
            }
        }

        if (sheet.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(sheet);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar la ficha técnica a JSON", e);
        }
    }
}
