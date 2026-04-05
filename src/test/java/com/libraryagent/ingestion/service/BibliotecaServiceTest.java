package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.VerifiedTitleDto;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BibliotecaServiceTest {

    @Mock
    VerifiedTitleRepository verifiedTitleRepository;

    @Mock
    ExtractedBookRepository extractedBookRepository;

    @InjectMocks
    BibliotecaServiceImpl service;

    @Test
    void shouldReturnEmptyListWhenNoVerifiedTitles() {
        // Given
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        // When
        List<VerifiedTitleDto> result = service.findAll();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnVerifiedTitlesOrderedByName() {
        // Given
        VerifiedTitleEntity dune = buildVerifiedTitle("Dune", null, null, null);
        VerifiedTitleEntity fundacion = buildVerifiedTitle("Fundación", null, null, null);
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dune, fundacion));
        when(extractedBookRepository.findByVerifiedTitleAndConfidence(dune, Confidence.VERIFIED))
                .thenReturn(List.of());
        when(extractedBookRepository.findByVerifiedTitleAndConfidence(fundacion, Confidence.VERIFIED))
                .thenReturn(List.of());

        // When
        List<VerifiedTitleDto> result = service.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Dune");
        assertThat(result.get(1).name()).isEqualTo("Fundación");
    }

    @Test
    void shouldAggregateDistinctAuthorsFromVerifiedBooks() {
        // Given
        VerifiedTitleEntity dune = buildVerifiedTitle("Dune", null, null, null);
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dune));

        AuthorEntity frank = buildAuthor("Frank Herbert");
        ExtractedBookEntity book1 = buildBookWithAuthors(List.of(frank));
        ExtractedBookEntity book2 = buildBookWithAuthors(List.of(frank)); // mismo autor en otro libro
        when(extractedBookRepository.findByVerifiedTitleAndConfidence(dune, Confidence.VERIFIED))
                .thenReturn(List.of(book1, book2));

        // When
        List<VerifiedTitleDto> result = service.findAll();

        // Then — el autor aparece una sola vez aunque esté en varios libros
        assertThat(result).hasSize(1);
        assertThat(result.get(0).authors()).containsExactly("Frank Herbert");
    }

    @Test
    void shouldExposeGoogleBooksFieldsFromEntity() {
        // Given
        VerifiedTitleEntity vt = buildVerifiedTitle("Dune", "https://cover.url/dune.jpg",
                "Sinopsis de Dune", "volXYZ");
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(vt));
        when(extractedBookRepository.findByVerifiedTitleAndConfidence(vt, Confidence.VERIFIED))
                .thenReturn(List.of());

        // When
        List<VerifiedTitleDto> result = service.findAll();

        // Then
        VerifiedTitleDto dto = result.get(0);
        assertThat(dto.coverUrl()).isEqualTo("https://cover.url/dune.jpg");
        assertThat(dto.synopsis()).isEqualTo("Sinopsis de Dune");
        assertThat(dto.googleBooksId()).isEqualTo("volXYZ");
    }

    @Test
    void shouldReturnSortedAuthorsAlphabetically() {
        // Given
        VerifiedTitleEntity vt = buildVerifiedTitle("Good Omens", null, null, null);
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(vt));

        AuthorEntity gaiman = buildAuthor("Neil Gaiman");
        AuthorEntity pratchett = buildAuthor("Terry Pratchett");
        ExtractedBookEntity book = buildBookWithAuthors(List.of(pratchett, gaiman));
        when(extractedBookRepository.findByVerifiedTitleAndConfidence(vt, Confidence.VERIFIED))
                .thenReturn(List.of(book));

        // When
        List<VerifiedTitleDto> result = service.findAll();

        // Then — autores ordenados alfabéticamente
        assertThat(result.get(0).authors()).containsExactly("Neil Gaiman", "Terry Pratchett");
    }

    // --- Helpers ---

    private VerifiedTitleEntity buildVerifiedTitle(String name, String coverUrl, String synopsis, String googleBooksId) {
        VerifiedTitleEntity vt = new VerifiedTitleEntity(name);
        vt.setCoverUrl(coverUrl);
        vt.setSynopsis(synopsis);
        vt.setGoogleBooksId(googleBooksId);
        return vt;
    }

    private AuthorEntity buildAuthor(String name) {
        AuthorEntity author = new AuthorEntity(name);
        return author;
    }

    private ExtractedBookEntity buildBookWithAuthors(List<AuthorEntity> authors) {
        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setId(UUID.randomUUID());
        book.getAuthors().addAll(authors);
        return book;
    }
}
