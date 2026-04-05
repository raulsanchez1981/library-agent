package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.VerifiedTitleDto;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
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
        // findVerifiedTitleIdAndAuthorNameByConfidence devuelve lista vacía por defecto (Mockito)

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
        when(extractedBookRepository.findVerifiedTitleIdAndAuthorNameByConfidence(Confidence.VERIFIED))
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
        UUID duneId = UUID.randomUUID();
        VerifiedTitleEntity dune = buildVerifiedTitleWithId(duneId, "Dune", null, null, null);
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dune));

        // El mismo autor aparece en dos libros distintos — debe deduplicarse
        when(extractedBookRepository.findVerifiedTitleIdAndAuthorNameByConfidence(Confidence.VERIFIED))
                .thenReturn(List.of(
                        new Object[]{duneId, "Frank Herbert"},
                        new Object[]{duneId, "Frank Herbert"}
                ));

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
        when(extractedBookRepository.findVerifiedTitleIdAndAuthorNameByConfidence(Confidence.VERIFIED))
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
        UUID vtId = UUID.randomUUID();
        VerifiedTitleEntity vt = buildVerifiedTitleWithId(vtId, "Good Omens", null, null, null);
        when(verifiedTitleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(vt));

        when(extractedBookRepository.findVerifiedTitleIdAndAuthorNameByConfidence(Confidence.VERIFIED))
                .thenReturn(List.of(
                        new Object[]{vtId, "Terry Pratchett"},
                        new Object[]{vtId, "Neil Gaiman"}
                ));

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

    private VerifiedTitleEntity buildVerifiedTitleWithId(UUID id, String name, String coverUrl, String synopsis, String googleBooksId) {
        VerifiedTitleEntity vt = buildVerifiedTitle(name, coverUrl, synopsis, googleBooksId);
        vt.setId(id);
        return vt;
    }
}
