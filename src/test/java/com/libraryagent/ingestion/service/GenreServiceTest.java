package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    GenreRepository genreRepository;

    GenreService service;

    @BeforeEach
    void setUp() {
        service = new GenreServiceImpl(genreRepository);
    }

    @Test
    void shouldReturnAllGenresSortedByNameAsc() {
        // Given
        GenreEntity fantasia = new GenreEntity("Fantasía");
        GenreEntity aventura = new GenreEntity("Aventura");
        when(genreRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(aventura, fantasia));

        // When
        List<GenreEntity> result = service.findAll();

        // Then
        assertThat(result).containsExactly(aventura, fantasia);
    }

    @Test
    void shouldReturnExistingGenreWhenNameMatchesIgnoreCase() {
        // Given
        GenreEntity existing = new GenreEntity("Fantasía");
        when(genreRepository.findByNameIgnoreCase("fantasía")).thenReturn(Optional.of(existing));

        // When
        GenreEntity result = service.findOrCreate("fantasía");

        // Then
        assertThat(result).isEqualTo(existing);
        verify(genreRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewGenreWhenNameDoesNotExist() {
        // Given
        GenreEntity newGenre = new GenreEntity("Ciencia Ficción");
        when(genreRepository.findByNameIgnoreCase("Ciencia Ficción")).thenReturn(Optional.empty());
        when(genreRepository.save(any(GenreEntity.class))).thenReturn(newGenre);

        // When
        GenreEntity result = service.findOrCreate("Ciencia Ficción");

        // Then
        assertThat(result.getName()).isEqualTo("Ciencia Ficción");
        verify(genreRepository).save(any(GenreEntity.class));
    }
}
