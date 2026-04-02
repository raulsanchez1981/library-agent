package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.extractor.ClaudeGateway;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.extractor.OpenLibraryClient;
import com.libraryagent.ingestion.extractor.SpanishEdition;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookEnrichmentServiceTest {

    @Mock ExtractedBookRepository repository;
    @Mock AuthorRepository authorRepository;
    @Mock ClaudeGateway claudeGateway;
    @Mock OpenLibraryClient openLibraryClient;
    @Mock ExtractedBookAdminService extractedBookAdminService;

    BookEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new BookEnrichmentService(repository, authorRepository, claudeGateway, openLibraryClient, new ObjectMapper(), extractedBookAdminService);
    }

    // ── Caso 1: SONNET + HIGH ─────────────────────────────────────────────────

    @Test
    void shouldMarkAsSonnetHighWhenOLTitleMatchesSonnetTitle() {
        // Given
        ExtractedBookEntity entity = pendingBook("The Lord of the Rings", "Tolkien");
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"El Señor de los Anillos\",\"authorCorrected\":\"J.R.R. Tolkien\",\"isSaga\":true}]");
        when(openLibraryClient.findBySpanishTitle("El Señor de los Anillos"))
                .thenReturn(Optional.of(new SpanishEdition("El Señor de los Anillos", "The Lord of the Rings", "J.R.R. Tolkien", true)));

        // When
        service.enrichPending();

        // Then
        assertThat(entity.getTitleEs()).isEqualTo("El Señor de los Anillos");
        assertThat(entity.getEnrichmentSource()).isEqualTo(EnrichmentSource.SONNET);
        assertThat(entity.getConfidence()).isEqualTo(Confidence.HIGH);
        assertThat(entity.isAvailableInSpanish()).isTrue();
        assertThat(entity.getAuthorCorrected()).isEqualTo("J.R.R. Tolkien");
        assertThat(entity.isEnriched()).isTrue();
        assertThat(entity.getEnrichedAt()).isNotNull();
        verify(repository).saveAll(any());
    }

    // ── Caso 2: SONNET + MEDIUM ───────────────────────────────────────────────

    @Test
    void shouldMarkAsSonnetMediumWhenOLFindsNothing() {
        // Given — Sonnet conoce la traducción pero OL no encuentra nada
        ExtractedBookEntity entity = pendingBook("Stormlight Archive Series", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"El Archivo de las Tormentas\",\"authorCorrected\":\"Brandon Sanderson\",\"isSaga\":true}]");
        when(openLibraryClient.findBySpanishTitle("El Archivo de las Tormentas"))
                .thenReturn(Optional.empty());

        // When
        service.enrichPending();

        // Then
        assertThat(entity.getTitleEs()).isEqualTo("El Archivo de las Tormentas");
        assertThat(entity.getTitleEsOl()).isNull();
        assertThat(entity.getEnrichmentSource()).isEqualTo(EnrichmentSource.SONNET);
        assertThat(entity.getConfidence()).isEqualTo(Confidence.MEDIUM);
        assertThat(entity.isAvailableInSpanish()).isTrue();
        assertThat(entity.getAuthorCorrected()).isEqualTo("Brandon Sanderson");
        assertThat(entity.isEnriched()).isTrue();
    }

    // ── Caso 3: SONNET + LOW ──────────────────────────────────────────────────

    @Test
    void shouldMarkAsSonnetLowWhenOLTitleDifferent() {
        // Given — Sonnet devuelve "Los magos", OL devuelve "El Archimago de Westeros"
        // Las palabras clave únicas son: "magos" vs "archimago", "westeros" → sin solapamiento
        ExtractedBookEntity entity = pendingBook("The Magicians", "Lev Grossman");
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"Los magos\",\"authorCorrected\":null,\"isSaga\":false}]");
        when(openLibraryClient.findBySpanishTitle("Los magos"))
                .thenReturn(Optional.of(new SpanishEdition("El Archimago de Westeros", "The Magicians", "Lev Grossman", true)));

        // When
        service.enrichPending();

        // Then — baja similitud → LOW
        assertThat(entity.getTitleEs()).isEqualTo("Los magos");
        assertThat(entity.getTitleEsOl()).isEqualTo("El Archimago de Westeros");
        assertThat(entity.getEnrichmentSource()).isEqualTo(EnrichmentSource.SONNET);
        assertThat(entity.getConfidence()).isEqualTo(Confidence.LOW);
        assertThat(entity.isAvailableInSpanish()).isTrue();
        assertThat(entity.isEnriched()).isTrue();
    }

    // ── Caso 4: OL_ONLY ───────────────────────────────────────────────────────

    @Test
    void shouldMarkAsOlOnlyWhenSonnetNullButOLFindsEdition() {
        // Given — Sonnet no conoce la traducción, OL sí la tiene
        ExtractedBookEntity entity = pendingBook("Gridlinked", "Neal Asher");
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":null,\"authorCorrected\":null,\"isSaga\":false}]");
        when(openLibraryClient.findSpanishEdition("Gridlinked"))
                .thenReturn(Optional.of(new SpanishEdition("Gridlinked en español", "Gridlinked", "Neal Asher", true)));

        // When
        service.enrichPending();

        // Then
        assertThat(entity.getTitleEs()).isEqualTo("Gridlinked en español");
        assertThat(entity.getEnrichmentSource()).isEqualTo(EnrichmentSource.OL_ONLY);
        assertThat(entity.getConfidence()).isNull();
        assertThat(entity.isAvailableInSpanish()).isTrue();
        assertThat(entity.isEnriched()).isTrue();
        verify(openLibraryClient, never()).findBySpanishTitle(anyString());
    }

    // ── Caso 5: NONE ──────────────────────────────────────────────────────────

    @Test
    void shouldMarkAsNoneWhenNeitherSourceFindsTranslation() {
        // Given — ninguna fuente conoce la traducción
        ExtractedBookEntity entity = pendingBook("Dungeon Crawler Carl", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":null,\"authorCorrected\":null,\"isSaga\":false}]");
        when(openLibraryClient.findSpanishEdition("Dungeon Crawler Carl"))
                .thenReturn(Optional.empty());

        // When
        service.enrichPending();

        // Then
        assertThat(entity.getTitleEs()).isNull();
        assertThat(entity.getEnrichmentSource()).isEqualTo(EnrichmentSource.NONE);
        assertThat(entity.getConfidence()).isNull();
        assertThat(entity.isAvailableInSpanish()).isFalse();
        assertThat(entity.isEnriched()).isTrue();
    }

    // ── Casos de infraestructura ──────────────────────────────────────────────

    @Test
    void shouldProcessInBatchesOfTen() {
        // Given — 12 libros → 2 llamadas a Sonnet (10 + 2)
        List<ExtractedBookEntity> books = generateBooks(12);
        when(repository.findByEnrichedFalse()).thenReturn(books);

        String batchFull = buildSonnetResponse(10);
        String batchRest = buildSonnetResponse(2);
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn(batchFull).thenReturn(batchRest);
        when(openLibraryClient.findSpanishEdition(anyString())).thenReturn(Optional.empty());

        // When
        service.enrichPending();

        // Then — 2 llamadas a Sonnet, todos los libros marcados como enriquecidos
        verify(claudeGateway, times(2)).enrichBooksBatchJson(anyString());
        assertThat(books).allSatisfy(b -> assertThat(b.isEnriched()).isTrue());
    }

    @Test
    void shouldDoNothingWhenNoPendingBooks() {
        // Given
        when(repository.findByEnrichedFalse()).thenReturn(List.of());

        // When
        service.enrichPending();

        // Then
        verify(claudeGateway, never()).enrichBooksBatchJson(anyString());
        verify(repository, never()).saveAll(any());
        verify(extractedBookAdminService, never()).linkUnverifiedBooks();
    }

    @Test
    void shouldCallLinkUnverifiedBooksAfterEnrichingBatch() {
        // Given
        ExtractedBookEntity entity = pendingBook("Dune", "Frank Herbert");
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"Dune\",\"authorCorrected\":\"Frank Herbert\",\"isSaga\":false}]");
        when(openLibraryClient.findBySpanishTitle("Dune")).thenReturn(Optional.empty());

        // When
        service.enrichPending();

        // Then — linkUnverifiedBooks se invoca una vez, al final del pipeline
        verify(extractedBookAdminService, times(1)).linkUnverifiedBooks();
    }

    @Test
    void shouldSetOlAuthorWhenEntityAuthorNullAndOlFindsIt() {
        // Given
        ExtractedBookEntity entity = pendingBook("Obscure Book", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":null,\"authorCorrected\":null,\"isSaga\":false}]");
        when(openLibraryClient.findSpanishEdition("Obscure Book"))
                .thenReturn(Optional.of(new SpanishEdition("Libro Oscuro", "Obscure Book", "OL Author", true)));

        // When
        service.enrichPending();

        // Then — autor de OL guardado tanto en authorCorrected como en author
        assertThat(entity.getAuthorCorrected()).isEqualTo("OL Author");
        assertThat(entity.getAuthor()).isEqualTo("OL Author");
        assertThat(entity.getEnrichmentSource()).isEqualTo(EnrichmentSource.OL_ONLY);
        assertThat(entity.getConfidence()).isNull();
    }

    // ── Bug fix: authorCorrected de Sonnet se copia a author cuando author es null ──

    @Test
    void shouldCopyAuthorCorrectedToAuthorWhenAuthorIsNull() {
        // Given — libro sin autor; Sonnet devuelve authorCorrected
        ExtractedBookEntity entity = pendingBook("The Name of the Wind", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"El nombre del viento\",\"authorCorrected\":\"Patrick Rothfuss\",\"isSaga\":false}]");
        when(openLibraryClient.findBySpanishTitle("El nombre del viento"))
                .thenReturn(Optional.empty());

        // When
        service.enrichPending();

        // Then — author queda relleno con el valor corregido de Sonnet
        assertThat(entity.getAuthorCorrected()).isEqualTo("Patrick Rothfuss");
        assertThat(entity.getAuthor()).isEqualTo("Patrick Rothfuss");
        assertThat(entity.isEnriched()).isTrue();
    }

    // ── reEnrichAuthors ───────────────────────────────────────────────────────

    @Test
    void shouldReturnCountOfRecoveredAuthors() {
        // Given — dos libros enriquecidos sin autor; Sonnet recupera uno de ellos
        ExtractedBookEntity book1 = pendingBook("Book Without Author 1", null);
        book1.setEnriched(true);
        ExtractedBookEntity book2 = pendingBook("Book Without Author 2", null);
        book2.setEnriched(true);

        when(repository.findByEnrichedTrueAndAuthorIsNull()).thenReturn(List.of(book1, book2));
        when(claudeGateway.lookupAuthorsBatchJson(anyString()))
                .thenReturn("[{\"author\":\"Brandon Sanderson\"},{\"author\":null}]");

        // When
        int result = service.reEnrichAuthors();

        // Then — solo book1 recuperó autor
        assertThat(result).isEqualTo(1);
        assertThat(book1.getAuthor()).isEqualTo("Brandon Sanderson");
        assertThat(book1.getAuthorCorrected()).isEqualTo("Brandon Sanderson");
        assertThat(book2.getAuthor()).isNull();
        verify(repository).saveAll(any());
    }

    // ── Author find-or-create ─────────────────────────────────────────────────

    @Test
    void shouldCreateAuthorEntityWhenAuthorCorrectedIsSet() {
        // Given
        ExtractedBookEntity entity = pendingBook("The Name of the Wind", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"El nombre del viento\",\"authorCorrected\":\"Patrick Rothfuss\",\"isSaga\":false}]");
        when(openLibraryClient.findBySpanishTitle("El nombre del viento")).thenReturn(Optional.empty());

        AuthorEntity authorEntity = new AuthorEntity("Patrick Rothfuss");
        when(authorRepository.findByNameIgnoreCase("Patrick Rothfuss")).thenReturn(Optional.empty());
        when(authorRepository.save(any(AuthorEntity.class))).thenReturn(authorEntity);

        // When
        service.enrichPending();

        // Then — se crea el autor y se asocia al libro
        assertThat(entity.getAuthors()).hasSize(1);
        assertThat(entity.getAuthors().get(0).getName()).isEqualTo("Patrick Rothfuss");
        verify(authorRepository).save(any(AuthorEntity.class));
    }

    @Test
    void shouldReuseExistingAuthorEntityWhenAuthorAlreadyExists() {
        // Given
        ExtractedBookEntity entity = pendingBook("The Final Empire", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"El Imperio Final\",\"authorCorrected\":\"Brandon Sanderson\",\"isSaga\":false}]");
        when(openLibraryClient.findBySpanishTitle("El Imperio Final")).thenReturn(Optional.empty());

        AuthorEntity existing = new AuthorEntity("Brandon Sanderson");
        when(authorRepository.findByNameIgnoreCase("Brandon Sanderson")).thenReturn(Optional.of(existing));

        // When
        service.enrichPending();

        // Then — se recupera el existente, no se crea uno nuevo
        assertThat(entity.getAuthors()).hasSize(1);
        verify(authorRepository, never()).save(any(AuthorEntity.class));
    }

    @Test
    void shouldParseMultipleAuthorsFromAuthorCorrected() {
        // Given — obra con dos autores separados por " & "
        ExtractedBookEntity entity = pendingBook("Good Omens", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":\"Buenos presagios\",\"authorCorrected\":\"Terry Pratchett & Neil Gaiman\",\"isSaga\":false}]");
        when(openLibraryClient.findBySpanishTitle("Buenos presagios")).thenReturn(Optional.empty());

        AuthorEntity pratchett = new AuthorEntity("Terry Pratchett");
        AuthorEntity gaiman = new AuthorEntity("Neil Gaiman");
        when(authorRepository.findByNameIgnoreCase("Terry Pratchett")).thenReturn(Optional.empty());
        when(authorRepository.findByNameIgnoreCase("Neil Gaiman")).thenReturn(Optional.empty());
        when(authorRepository.save(any(AuthorEntity.class))).thenReturn(pratchett).thenReturn(gaiman);

        // When
        service.enrichPending();

        // Then — ambos autores se crean y asocian
        assertThat(entity.getAuthors()).hasSize(2);
        verify(authorRepository, times(2)).save(any(AuthorEntity.class));
    }

    @Test
    void shouldNotCreateAuthorsWhenAuthorCorrectedIsNull() {
        // Given — Sonnet no conoce al autor ni la traducción
        ExtractedBookEntity entity = pendingBook("Dungeon Crawler Carl", null);
        when(repository.findByEnrichedFalse()).thenReturn(List.of(entity));
        when(claudeGateway.enrichBooksBatchJson(anyString()))
                .thenReturn("[{\"titleEs\":null,\"authorCorrected\":null,\"isSaga\":false}]");
        when(openLibraryClient.findSpanishEdition("Dungeon Crawler Carl")).thenReturn(Optional.empty());

        // When
        service.enrichPending();

        // Then — no se interactúa con authorRepository
        assertThat(entity.getAuthors()).isEmpty();
        verifyNoInteractions(authorRepository);
    }

    // --- Helpers ---

    private ExtractedBookEntity pendingBook(String title, String author) {
        ExtractedBookEntity e = new ExtractedBookEntity();
        e.setTitle(title);
        e.setAuthor(author);
        e.setSaga(false);
        e.setEnriched(false);
        return e;
    }

    private List<ExtractedBookEntity> generateBooks(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> pendingBook("Book" + i, null))
                .toList();
    }

    private String buildSonnetResponse(int count) {
        String entry = "{\"titleEs\":null,\"authorCorrected\":null,\"isSaga\":false}";
        return "[" + String.join(",", Collections.nCopies(count, entry)) + "]";
    }
}
