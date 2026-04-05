package com.libraryagent.ingestion.extractor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RestClientOpenLibraryClient implements OpenLibraryClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientOpenLibraryClient.class);
    private static final String BASE_URL = "https://openlibrary.org";

    /**
     * Umbral mínimo de similitud para aceptar un resultado de OL como válido.
     * El 80% de las palabras significativas del título buscado deben aparecer en el resultado.
     */
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.8;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "of", "in", "at", "to", "for", "on", "by", "with", "is"
    );

    private final RestClient restClient;

    public RestClientOpenLibraryClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * Busca partiendo del título en inglés (sin filtro de idioma para mejor precisión),
     * valida similitud y devuelve la primera edición en español encontrada.
     * Usado como fallback cuando Sonnet no conoce la traducción.
     */
    @Override
    public Optional<SpanishEdition> findSpanishEdition(String englishTitle) {
        try {
            SearchDoc doc = findWorkByTitle(englishTitle, false);
            if (doc == null) return Optional.empty();

            String author = extractAuthor(doc);
            String titleEs = findSpanishTitle(doc.key());
            if (titleEs == null) return Optional.empty();

            return Optional.of(new SpanishEdition(titleEs, englishTitle, author, true));

        } catch (RestClientException e) {
            log.warn("Error al consultar OpenLibrary para '{}': {}", englishTitle, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca en OL usando el título en español con filtro language=spa.
     * Devuelve el título en español que OL tiene catalogado (puede diferir del buscado).
     * Usado para confirmar o contrastar la traducción propuesta por Sonnet.
     */
    @Override
    public Optional<SpanishEdition> findBySpanishTitle(String spanishTitle) {
        try {
            SearchDoc doc = findWorkByTitle(spanishTitle, true);
            if (doc == null) return Optional.empty();

            String author = extractAuthor(doc);
            String titleEs = findSpanishTitle(doc.key());
            if (titleEs == null) return Optional.empty();

            return Optional.of(new SpanishEdition(titleEs, doc.title(), author, true));

        } catch (RestClientException e) {
            log.warn("Error al verificar título en español '{}' en OpenLibrary: {}", spanishTitle, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> findCoverUrl(String title, String originalTitle) {
        // 1. Título en español con filtro español
        Optional<String> url = coverUrlFromTitle(title, true);
        if (url.isPresent()) return url;

        // 2. Título en español sin filtro
        url = coverUrlFromTitle(title, false);
        if (url.isPresent()) return url;

        // 3. Título original en inglés
        if (originalTitle != null && !originalTitle.equalsIgnoreCase(title)) {
            url = coverUrlFromTitle(originalTitle, false);
            if (url.isPresent()) return url;
        }

        return Optional.empty();
    }

    private Optional<String> coverUrlFromTitle(String title, boolean useSpanishFilter) {
        try {
            SearchDoc doc = findWorkByTitle(title, useSpanishFilter);
            if (doc == null || doc.coverId() == null) return Optional.empty();
            return Optional.of("https://covers.openlibrary.org/b/id/" + doc.coverId() + "-L.jpg");
        } catch (RestClientException e) {
            log.warn("Error al buscar portada en OpenLibrary para '{}': {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca en OL el work cuyo título sea más similar al buscado.
     * Si useSpanishFilter=true incluye &language=spa (para búsquedas por título español).
     */
    private SearchDoc findWorkByTitle(String title, boolean useSpanishFilter) {
        String uri = useSpanishFilter
                ? "/search.json?title={title}&language=spa&limit=5&fields=key,title,author_name,cover_i"
                : "/search.json?title={title}&limit=5&fields=key,title,author_name,cover_i";

        SearchResponse response = restClient.get()
                .uri(uri, title)
                .retrieve()
                .body(SearchResponse.class);

        if (response == null || response.docs() == null || response.docs().isEmpty()) return null;

        return response.docs().stream()
                .filter(doc -> titleSimilarity(doc.title(), title) >= TITLE_SIMILARITY_THRESHOLD)
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtiene el título de la primera edición en español del work indicado.
     */
    private String findSpanishTitle(String workKey) {
        EditionsResponse editions = restClient.get()
                .uri(workKey + "/editions.json?limit=100")
                .retrieve()
                .body(EditionsResponse.class);

        if (editions == null || editions.entries() == null) return null;

        return editions.entries().stream()
                .filter(e -> e.languages() != null && e.languages().stream()
                        .anyMatch(l -> "/languages/spa".equals(l.key())))
                .map(EditionEntry::title)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extractAuthor(SearchDoc doc) {
        return (doc.authorName() != null && !doc.authorName().isEmpty())
                ? doc.authorName().getFirst() : null;
    }

    /**
     * Para títulos de una sola palabra significativa exige coincidencia exacta
     * (evita que "Cradle" encaje con "Cat's Cradle").
     * Para títulos de múltiples palabras exige que al menos el 80% aparezca en OL.
     */
    private double titleSimilarity(String olTitle, String searchTitle) {
        Set<String> searchTokens = significantTokens(searchTitle);
        if (searchTokens.isEmpty()) return 0;
        Set<String> olTokens = significantTokens(olTitle);

        if (searchTokens.size() == 1) {
            return searchTokens.equals(olTokens) ? 1.0 : 0.0;
        }

        long common = searchTokens.stream().filter(olTokens::contains).count();
        return (double) common / searchTokens.size();
    }

    private Set<String> significantTokens(String title) {
        if (title == null) return Set.of();
        String normalized = title.toLowerCase().replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank() && !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
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
            @JsonProperty("author_name") List<String> authorName,
            @JsonProperty("cover_i") Integer coverId
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
