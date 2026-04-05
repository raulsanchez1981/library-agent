package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.ExtractedBookAdminDto;
import com.libraryagent.ingestion.dto.UpdateExtractedBookRequest;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractedBookAdminServiceTest {

    @Mock
    ExtractedBookRepository repository;

    @Mock
    AuthorRepository authorRepository;

    @Mock
    VerifiedTitleRepository verifiedTitleRepository;

    @Mock
    GenreEnrichmentService genreEnrichmentService;

    @InjectMocks
    ExtractedBookAdminServiceImpl service;

    @Test
    void shouldReturnPageOfBooksWhenNoFilters() {
        // Given
        ExtractedBookEntity entity = buildEntity(UUID.randomUUID(), "Dune", "Frank Herbert");
        Page<ExtractedBookEntity> entityPage = new PageImpl<>(List.of(entity));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

        // When
        Page<ExtractedBookAdminDto> result = service.findAll(null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Dune");
        assertThat(result.getContent().get(0).author()).isEqualTo("Frank Herbert");
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldThrowWhenBookNotFound() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void shouldUpdateOnlyNonNullFields() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");
        entity.setTitleEs("Dune (ES original)");
        entity.setConfidence(Confidence.LOW);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        AuthorEntity fHerbert = new AuthorEntity("F. Herbert");
        when(authorRepository.findByNameIgnoreCase("F. Herbert")).thenReturn(Optional.empty());
        when(authorRepository.save(any(AuthorEntity.class))).thenReturn(fHerbert);

        // Mocks para linkUnverifiedBooks()
        VerifiedTitleEntity vt = new VerifiedTitleEntity("Dune");
        when(verifiedTitleRepository.findByNameIgnoreCase("Dune")).thenReturn(Optional.of(vt));
        stubLinkUnverifiedBooks();


        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest(
                "Dune",       // titleEs actualizado
                "F. Herbert", // authorCorrected actualizado
                true,         // availableInSpanish actualizado
                null          // isSaga — no modificar
        );

        // When
        ExtractedBookAdminDto result = service.update(id, request);

        // Then
        assertThat(result.titleEs()).isEqualTo("Dune");
        assertThat(result.authorCorrected()).isEqualTo("F. Herbert");
        assertThat(result.confidence()).isEqualTo(Confidence.VERIFIED);
        assertThat(result.enrichmentSource()).isEqualTo(EnrichmentSource.ADMIN);
        assertThat(result.availableInSpanish()).isTrue();
        verify(repository).save(entity);
    }

    @Test
    void shouldNotModifyFieldWhenUpdateRequestFieldIsNull() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");
        entity.setTitleEs("Dune (ES previo)");
        entity.setAuthorCorrected("Frank Herbert previo");
        entity.setConfidence(Confidence.MEDIUM);
        entity.setAvailableInSpanish(false);
        entity.setSaga(true);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        // Mocks para linkUnverifiedBooks() — titleEs es null así que no crea VerifiedTitle
        stubLinkUnverifiedBooks();

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest(null, null, null, null);

        // When
        ExtractedBookAdminDto result = service.update(id, request);

        // Then — campos de datos sin cambio, pero el admin sella confidence y source
        assertThat(result.titleEs()).isEqualTo("Dune (ES previo)");
        assertThat(result.authorCorrected()).isEqualTo("Frank Herbert previo");
        assertThat(result.confidence()).isEqualTo(Confidence.VERIFIED);
        assertThat(result.enrichmentSource()).isEqualTo(EnrichmentSource.ADMIN);
        assertThat(result.availableInSpanish()).isFalse();
        assertThat(result.isSaga()).isTrue();
    }

    @Test
    void shouldAlwaysSetVerifiedAndAdminSourceRegardlessOfRequest() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Foundation", "Isaac Asimov");
        entity.setConfidence(Confidence.HIGH);
        entity.setEnrichmentSource(EnrichmentSource.SONNET);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        VerifiedTitleEntity vt = new VerifiedTitleEntity("Fundación");
        when(verifiedTitleRepository.findByNameIgnoreCase("Fundación")).thenReturn(Optional.of(vt));
        stubLinkUnverifiedBooks();


        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest("Fundación", null, null, null);

        // When
        ExtractedBookAdminDto result = service.update(id, request);

        // Then — independientemente de la confianza previa, el admin siempre sella
        assertThat(result.confidence()).isEqualTo(Confidence.VERIFIED);
        assertThat(result.enrichmentSource()).isEqualTo(EnrichmentSource.ADMIN);
    }

    @Test
    void shouldCreateAndAssociateAuthorsWhenAuthorCorrectedIsUpdated() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Good Omens", "Pratchett");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        AuthorEntity pratchett = new AuthorEntity("Terry Pratchett");
        AuthorEntity gaiman = new AuthorEntity("Neil Gaiman");
        when(authorRepository.findByNameIgnoreCase("Terry Pratchett")).thenReturn(Optional.empty());
        when(authorRepository.findByNameIgnoreCase("Neil Gaiman")).thenReturn(Optional.empty());
        when(authorRepository.save(any(AuthorEntity.class))).thenReturn(pratchett).thenReturn(gaiman);

        stubLinkUnverifiedBooks();

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest(null, "Terry Pratchett & Neil Gaiman", null, null);

        // When
        ExtractedBookAdminDto result = service.update(id, request);

        // Then — los dos autores se crean (2 saves) y luego se marcan verified (2 saves más)
        assertThat(result.authorCorrected()).isEqualTo("Terry Pratchett & Neil Gaiman");
        assertThat(entity.getAuthors()).hasSize(2);
        verify(authorRepository, times(4)).save(any(AuthorEntity.class));
    }

    @Test
    void shouldReuseExistingAuthorWhenUpdatingAuthorCorrected() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Foundation", "Asimov");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        AuthorEntity existing = new AuthorEntity("Isaac Asimov");
        when(authorRepository.findByNameIgnoreCase("Isaac Asimov")).thenReturn(Optional.of(existing));

        VerifiedTitleEntity vt = new VerifiedTitleEntity("Fundación");
        when(verifiedTitleRepository.findByNameIgnoreCase("Fundación")).thenReturn(Optional.of(vt));
        stubLinkUnverifiedBooks();


        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest("Fundación", "Isaac Asimov", null, null);

        // When
        service.update(id, request);

        // Then — el autor existente se reutiliza (no se crea uno nuevo) y se marca como verified
        assertThat(entity.getAuthors()).hasSize(1);
        assertThat(entity.getAuthors().get(0)).isEqualTo(existing);
        // save se llama exactamente una vez: solo para marcar verified, no para crear un autor nuevo
        verify(authorRepository, times(1)).save(existing);
    }

    @Test
    void shouldReplaceAuthorsListWhenAuthorCorrectedChanges() {
        // Given — libro que ya tenía un autor asociado
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");
        entity.getAuthors().add(new AuthorEntity("Frank Herbert"));

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        AuthorEntity newAuthor = new AuthorEntity("Brian Herbert");
        when(authorRepository.findByNameIgnoreCase("Brian Herbert")).thenReturn(Optional.of(newAuthor));

        stubLinkUnverifiedBooks();

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest(null, "Brian Herbert", null, null);

        // When
        service.update(id, request);

        // Then — la lista se reemplaza completamente con el nuevo autor
        assertThat(entity.getAuthors()).hasSize(1);
        assertThat(entity.getAuthors().get(0).getName()).isEqualTo("Brian Herbert");
    }

    @Test
    void shouldNotCreateAuthorWhenAuthorCorrectedIsNull() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        stubLinkUnverifiedBooks();

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest("Dune", null, null, null);
        when(verifiedTitleRepository.findByNameIgnoreCase("Dune")).thenReturn(Optional.empty());
        when(verifiedTitleRepository.save(any(VerifiedTitleEntity.class))).thenReturn(new VerifiedTitleEntity("Dune"));


        // When
        service.update(id, request);

        // Then — authorRepository nunca se llama para crear/buscar autores
        verify(authorRepository, never()).findByNameIgnoreCase(any());
        verify(authorRepository, never()).save(any());
    }

    @Test
    void shouldCreateVerifiedTitleWhenTitleEsIsProvided() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        VerifiedTitleEntity createdVt = new VerifiedTitleEntity("Dune");
        when(verifiedTitleRepository.findByNameIgnoreCase("Dune")).thenReturn(Optional.empty());
        when(verifiedTitleRepository.save(any(VerifiedTitleEntity.class))).thenReturn(createdVt);
        stubLinkUnverifiedBooks();


        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest("Dune", null, null, null);

        // When
        service.update(id, request);

        // Then — se crea el VerifiedTitle y se vincula al libro
        verify(verifiedTitleRepository).findByNameIgnoreCase("Dune");
        verify(verifiedTitleRepository).save(any(VerifiedTitleEntity.class));
        assertThat(entity.getVerifiedTitle()).isEqualTo(createdVt);
    }

    @Test
    void shouldReuseExistingVerifiedTitleWhenAlreadyExists() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Foundation", "Isaac Asimov");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        VerifiedTitleEntity existing = new VerifiedTitleEntity("Fundación");
        when(verifiedTitleRepository.findByNameIgnoreCase("Fundación")).thenReturn(Optional.of(existing));
        stubLinkUnverifiedBooks();


        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest("Fundación", null, null, null);

        // When
        service.update(id, request);

        // Then — se reutiliza sin crear uno nuevo
        verify(verifiedTitleRepository, never()).save(any());
        assertThat(entity.getVerifiedTitle()).isEqualTo(existing);
    }

    @Test
    void shouldMarkAuthorsAsVerifiedWhenUpdatingBook() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");
        AuthorEntity author = new AuthorEntity("Frank Herbert");
        // verified = false por defecto
        entity.getAuthors().add(author);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(authorRepository.save(author)).thenReturn(author);
        stubLinkUnverifiedBooks();

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest(null, null, null, null);

        // When
        service.update(id, request);

        // Then — el autor queda marcado como verified
        assertThat(author.isVerified()).isTrue();
        verify(authorRepository).save(author);
    }

    @Test
    void shouldNotSaveAuthorAgainIfAlreadyVerified() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");
        AuthorEntity author = new AuthorEntity("Frank Herbert");
        author.setVerified(true);
        entity.getAuthors().add(author);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        stubLinkUnverifiedBooks();

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest(null, null, null, null);

        // When
        service.update(id, request);

        // Then — no se guarda el autor porque ya estaba verified
        verify(authorRepository, never()).save(any());
    }

    @Test
    void shouldLinkBooksWithMatchingTitleEsToVerifiedTitle() {
        // Given
        UUID id = UUID.randomUUID();
        ExtractedBookEntity entity = buildEntity(id, "Dune", "Frank Herbert");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        VerifiedTitleEntity vt = new VerifiedTitleEntity("Dune");
        when(verifiedTitleRepository.findByNameIgnoreCase("Dune")).thenReturn(Optional.of(vt));


        // Libro sin título verificado que coincide por titleEs
        ExtractedBookEntity unlinkedBook = buildEntity(UUID.randomUUID(), "Dune", "F. Herbert");
        unlinkedBook.setTitleEs("Dune");
        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of(unlinkedBook));
        when(verifiedTitleRepository.findAll()).thenReturn(List.of(vt));
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());

        UpdateExtractedBookRequest request = new UpdateExtractedBookRequest("Dune", null, null, null);

        // When
        service.update(id, request);

        // Then — el libro sin título verificado queda vinculado
        assertThat(unlinkedBook.getVerifiedTitle()).isEqualTo(vt);
        verify(repository).save(unlinkedBook);
    }

    // --- Helpers ---

    private ExtractedBookEntity buildEntity(UUID id, String title, String author) {
        ExtractedBookEntity entity = new ExtractedBookEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setAuthor(author);
        entity.setSaga(false);
        entity.setEnriched(false);
        entity.setAvailableInSpanish(false);
        entity.setEnrichmentSource(EnrichmentSource.NONE);
        return entity;
    }

    /**
     * Mockea las llamadas de linkUnverifiedBooks() con listas vacías
     * para los tests que no prueban esa lógica.
     */
    private void stubLinkUnverifiedBooks() {
        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of());
        when(verifiedTitleRepository.findAll()).thenReturn(List.of());
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());
    }

}
