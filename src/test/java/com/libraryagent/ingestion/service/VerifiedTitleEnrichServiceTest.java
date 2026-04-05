package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifiedTitleEnrichServiceTest {

    @Mock
    VerifiedTitleRepository verifiedTitleRepository;

    @Mock
    CasaDelLibroScraperService scraperService;

    @Mock
    GenreService genreService;

    VerifiedTitleEnrichService service;

    @BeforeEach
    void setUp() {
        service = new VerifiedTitleEnrichServiceImpl(verifiedTitleRepository, scraperService, genreService);
    }

    @Test
    void shouldEnrichTitleWithScrapedDataAndSave() {
        // Given
        UUID id = UUID.randomUUID();
        VerifiedTitleEntity entity = new VerifiedTitleEntity("El Nombre del Viento");
        entity.setGenres(new ArrayList<>());

        GenreEntity fantasia = new GenreEntity("Fantasía");
        CdlEnrichmentResultDto scraped = new CdlEnrichmentResultDto(
                "https://img.casadellibro.com/portada.jpg",
                "Una sinopsis épica.",
                "{\"ISBN\":\"978-84-12345-67-8\"}",
                List.of("Fantasía")
        );

        when(verifiedTitleRepository.findByIdWithGenres(id)).thenReturn(Optional.of(entity));
        when(scraperService.scrape(any())).thenReturn(scraped);
        when(genreService.findOrCreate("Fantasía")).thenReturn(fantasia);
        when(verifiedTitleRepository.save(entity)).thenReturn(entity);

        // When
        VerifiedTitleDetailDto result = service.enrichFromCdl(id, "https://www.casadellibro.com/libro");

        // Then
        assertThat(result.coverUrl()).isEqualTo("https://img.casadellibro.com/portada.jpg");
        assertThat(result.synopsis()).isEqualTo("Una sinopsis épica.");
        assertThat(result.genres()).hasSize(1);
        assertThat(result.genres().get(0).name()).isEqualTo("Fantasía");
        verify(verifiedTitleRepository).save(entity);
    }

    @Test
    void shouldNotAddDuplicateGenresWhenGenreAlreadyExists() {
        // Given
        UUID id = UUID.randomUUID();
        VerifiedTitleEntity entity = new VerifiedTitleEntity("Dune");
        GenreEntity existingGenre = new GenreEntity("Fantasía");
        entity.setGenres(new ArrayList<>(List.of(existingGenre)));

        CdlEnrichmentResultDto scraped = new CdlEnrichmentResultDto(
                null, null, null, List.of("Fantasía")
        );

        when(verifiedTitleRepository.findByIdWithGenres(id)).thenReturn(Optional.of(entity));
        when(scraperService.scrape(any())).thenReturn(scraped);
        when(verifiedTitleRepository.save(entity)).thenReturn(entity);

        // When
        service.enrichFromCdl(id, "https://www.casadellibro.com/libro");

        // Then
        verify(genreService, never()).findOrCreate(any());
        assertThat(entity.getGenres()).hasSize(1);
    }

    @Test
    void shouldThrowEntityNotFoundExceptionWhenIdDoesNotExist() {
        // Given
        UUID id = UUID.randomUUID();
        when(verifiedTitleRepository.findByIdWithGenres(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.enrichFromCdl(id, "https://www.casadellibro.com/libro"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
