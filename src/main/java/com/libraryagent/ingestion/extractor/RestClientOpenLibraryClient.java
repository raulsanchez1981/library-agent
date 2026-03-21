package com.libraryagent.ingestion.extractor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class RestClientOpenLibraryClient implements OpenLibraryClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientOpenLibraryClient.class);
    private static final String BASE_URL = "https://openlibrary.org";

    private final RestClient restClient;

    public RestClientOpenLibraryClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public Optional<SpanishEdition> findSpanishEdition(String englishTitle) {
        try {
            // Paso 1: buscar el work con ediciones en español para obtener key y autor
            SearchResponse response = restClient.get()
                    .uri("/search.json?title={title}&language=spa&limit=1&fields=key,title,author_name",
                            englishTitle)
                    .retrieve()
                    .body(SearchResponse.class);

            if (response == null || response.numFound() == 0 || response.docs().isEmpty()) {
                return Optional.of(new SpanishEdition(null, englishTitle, null, false));
            }

            SearchDoc doc = response.docs().getFirst();
            String author = (doc.authorName() != null && !doc.authorName().isEmpty())
                    ? doc.authorName().getFirst()
                    : null;

            // Paso 2: buscar el título de la edición en español en las ediciones del work
            String titleEs = findSpanishTitle(doc.key(), englishTitle);

            return Optional.of(new SpanishEdition(titleEs, englishTitle, author, true));

        } catch (RestClientException e) {
            log.warn("Error al consultar OpenLibrary para '{}': {}", englishTitle, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca el título de la primera edición en español del work.
     * Devuelve null si no hay edición en español o si el título coincide con el inglés.
     */
    private String findSpanishTitle(String workKey, String englishTitle) {
        try {
            EditionsResponse editions = restClient.get()
                    .uri(workKey + "/editions.json?limit=100")
                    .retrieve()
                    .body(EditionsResponse.class);

            if (editions == null || editions.entries() == null) return null;

            return editions.entries().stream()
                    .filter(e -> e.languages() != null && e.languages().stream()
                            .anyMatch(l -> "/languages/spa".equals(l.key())))
                    .map(EditionEntry::title)
                    .filter(t -> t != null && !t.equalsIgnoreCase(englishTitle))
                    .findFirst()
                    .orElse(null);

        } catch (RestClientException e) {
            log.warn("Error al obtener ediciones del work {}: {}", workKey, e.getMessage());
            return null;
        }
    }

    // --- Tipos internos de parseo ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchResponse(
            @JsonProperty("numFound") int numFound,
            @JsonProperty("docs") List<SearchDoc> docs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchDoc(
            @JsonProperty("key") String key,
            @JsonProperty("title") String title,
            @JsonProperty("author_name") List<String> authorName
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EditionsResponse(
            @JsonProperty("entries") List<EditionEntry> entries
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EditionEntry(
            @JsonProperty("title") String title,
            @JsonProperty("languages") List<LanguageRef> languages
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LanguageRef(
            @JsonProperty("key") String key
    ) {}
}
