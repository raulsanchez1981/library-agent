package com.libraryagent.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.ClaudeGateway;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreEnrichmentServiceTest {

    @Mock
    VerifiedTitleRepository verifiedTitleRepository;

    @Mock
    ExtractedBookRepository extractedBookRepository;

    @Mock
    ClaudeGateway claudeGateway;

    @Mock
    GenreService genreService;

    GenreEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new GenreEnrichmentServiceImpl(
                verifiedTitleRepository,
                extractedBookRepository,
                claudeGateway,
                genreService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnZeroWhenNoTitlesWithoutGenres() {
        // Given
        when(verifiedTitleRepository.findAllWithoutGenres()).thenReturn(List.of());

        // When
        int result = service.enrichMissingGenres();

        // Then
        assertThat(result).isZero();
        verify(claudeGateway, never()).inferGenresBatchJson(anyString());
    }

    @Test
    void shouldEnrichGenresForTitlesWithoutGenres() {
        // Given
        VerifiedTitleEntity vt = new VerifiedTitleEntity();
        vt.setName("Dune");
        vt.setSynopsis("Historia de un joven noble en un planeta desértico.");

        when(verifiedTitleRepository.findAllWithoutGenres()).thenReturn(List.of(vt));
        when(extractedBookRepository.findByVerifiedTitleIdWithSearchData(any(), any()))
                .thenReturn(List.of());
        when(claudeGateway.inferGenresBatchJson(anyString()))
                .thenReturn("[{\"genres\":[\"Ciencia ficción\",\"Space opera\"]}]");

        GenreEntity genreSf = new GenreEntity();
        genreSf.setName("Ciencia ficción");
        GenreEntity genreSpace = new GenreEntity();
        genreSpace.setName("Space opera");

        when(genreService.findOrCreate("Ciencia ficción")).thenReturn(genreSf);
        when(genreService.findOrCreate("Space opera")).thenReturn(genreSpace);

        when(verifiedTitleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = service.enrichMissingGenres();

        // Then
        assertThat(result).isEqualTo(1);
        assertThat(vt.getGenres()).hasSize(2);
        verify(verifiedTitleRepository).save(vt);
    }

    @Test
    void shouldNotAddDuplicateGenres() {
        // Given
        GenreEntity existing = new GenreEntity();
        existing.setName("Ciencia ficción");

        VerifiedTitleEntity vt = new VerifiedTitleEntity();
        vt.setName("Fundación");
        vt.getGenres().add(existing);

        when(verifiedTitleRepository.findAllWithoutGenres()).thenReturn(List.of(vt));
        when(extractedBookRepository.findByVerifiedTitleIdWithSearchData(any(), any()))
                .thenReturn(List.of());
        // Haiku devuelve un género que ya existe y uno nuevo
        when(claudeGateway.inferGenresBatchJson(anyString()))
                .thenReturn("[{\"genres\":[\"Ciencia ficción\",\"Distopía\"]}]");

        GenreEntity genreDistopia = new GenreEntity();
        genreDistopia.setName("Distopía");
        when(genreService.findOrCreate("Distopía")).thenReturn(genreDistopia);

        when(verifiedTitleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int result = service.enrichMissingGenres();

        // Then
        assertThat(result).isEqualTo(1);
        // Solo se añade el género nuevo, no el duplicado
        assertThat(vt.getGenres()).hasSize(2);
        verify(genreService, never()).findOrCreate("Ciencia ficción");
        verify(genreService).findOrCreate("Distopía");
    }

    @Test
    void shouldEnrichAllGenresUsingFindAllWithGenres() {
        // Given
        when(verifiedTitleRepository.findAllWithGenres()).thenReturn(List.of());

        // When
        service.enrichAllGenres();

        // Then
        verify(verifiedTitleRepository).findAllWithGenres();
        verify(verifiedTitleRepository, never()).findAllWithoutGenres();
    }
}
