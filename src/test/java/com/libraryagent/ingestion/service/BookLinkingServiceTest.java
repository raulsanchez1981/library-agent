package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookLinkingServiceTest {

    @Mock
    ExtractedBookRepository repository;

    @Mock
    VerifiedTitleRepository verifiedTitleRepository;

    @Mock
    AuthorRepository authorRepository;

    @InjectMocks
    BookLinkingServiceImpl service;

    // ── Vinculación por titleEs ───────────────────────────────────────────────

    @Test
    void shouldLinkBookWhenTitleEsMatchesVerifiedTitle() {
        // Given
        VerifiedTitleEntity vt = verifiedTitle("Dune");
        ExtractedBookEntity book = bookWithTitleEs("Dune");

        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of(book));
        when(verifiedTitleRepository.findAll()).thenReturn(List.of(vt));
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());

        // When
        service.linkUnverifiedBooks();

        // Then
        assertThat(book.getVerifiedTitle()).isEqualTo(vt);
        verify(repository).saveAll(List.of(book));
    }

    @Test
    void shouldLinkCaseInsensitively() {
        // Given
        VerifiedTitleEntity vt = verifiedTitle("Dune");
        ExtractedBookEntity book = bookWithTitleEs("dune");

        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of(book));
        when(verifiedTitleRepository.findAll()).thenReturn(List.of(vt));
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());

        // When
        service.linkUnverifiedBooks();

        // Then
        assertThat(book.getVerifiedTitle()).isEqualTo(vt);
        verify(repository).saveAll(List.of(book));
    }

    @Test
    void shouldNotLinkWhenNoMatchExists() {
        // Given
        VerifiedTitleEntity vt = verifiedTitle("Fundación");
        ExtractedBookEntity book = bookWithTitleEs("Dune");

        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of(book));
        when(verifiedTitleRepository.findAll()).thenReturn(List.of(vt));
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());

        // When
        service.linkUnverifiedBooks();

        // Then
        assertThat(book.getVerifiedTitle()).isNull();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void shouldBulkSaveAllMatchedBooksInOneSaveAll() {
        // Given
        VerifiedTitleEntity vt = verifiedTitle("Dune");
        ExtractedBookEntity book1 = bookWithTitleEs("Dune");
        ExtractedBookEntity book2 = bookWithTitleEs("Dune");

        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of(book1, book2));
        when(verifiedTitleRepository.findAll()).thenReturn(List.of(vt));
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());

        // When
        service.linkUnverifiedBooks();

        // Then — un único saveAll con los dos libros, no N saves individuales
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExtractedBookEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(book1, book2);
    }

    @Test
    void shouldSkipQueryWhenNoBooksWithoutVerifiedTitle() {
        // Given
        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of());
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of());

        // When
        service.linkUnverifiedBooks();

        // Then — no se consultan los títulos verificados si no hay libros que vincular
        verify(verifiedTitleRepository, never()).findAll();
        verify(repository, never()).saveAll(any());
    }

    // ── Vinculación de autores verificados ───────────────────────────────────

    @Test
    void shouldLinkVerifiedAuthorToMatchingBook() {
        // Given
        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of());

        AuthorEntity autor = new AuthorEntity("Frank Herbert");
        autor.setVerified(true);
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of(autor));

        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setAuthorCorrected("Frank Herbert");
        book.setConfidence(Confidence.MEDIUM);
        book.getAuthors().clear();
        when(repository.findByConfidenceNotAndAuthorCorrectedIsNotNull(Confidence.VERIFIED))
                .thenReturn(List.of(book));

        // When
        service.linkUnverifiedBooks();

        // Then
        assertThat(book.getAuthors()).contains(autor);
        verify(repository).saveAll(List.of(book));
    }

    @Test
    void shouldNotDuplicateAuthorAlreadyLinked() {
        // Given
        when(repository.findByVerifiedTitleIsNullAndTitleEsIsNotNull()).thenReturn(List.of());

        AuthorEntity autor = new AuthorEntity("Frank Herbert");
        autor.setId(UUID.randomUUID());
        autor.setVerified(true);
        when(authorRepository.findByVerifiedTrue()).thenReturn(List.of(autor));

        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setAuthorCorrected("Frank Herbert");
        book.setConfidence(Confidence.MEDIUM);
        book.getAuthors().add(autor); // ya vinculado
        when(repository.findByConfidenceNotAndAuthorCorrectedIsNotNull(Confidence.VERIFIED))
                .thenReturn(List.of(book));

        // When
        service.linkUnverifiedBooks();

        // Then — no se vuelve a añadir ni se guarda
        assertThat(book.getAuthors()).hasSize(1);
        verify(repository, never()).saveAll(any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VerifiedTitleEntity verifiedTitle(String name) {
        return new VerifiedTitleEntity(name);
    }

    private ExtractedBookEntity bookWithTitleEs(String titleEs) {
        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setTitleEs(titleEs);
        return book;
    }
}
