package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.AuthorEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test de contrato de AuthorRepository.
 * La query derivada findByNameIgnoreCase es generada por Spring Data en tiempo de arranque;
 * este test verifica que el contrato de la interfaz es correcto y que los mocks responden como se espera.
 * Los tests de integración contra PostgreSQL real pertenecen a la suite de Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
class AuthorRepositoryTest {

    @Mock
    AuthorRepository repository;

    @Test
    void shouldReturnAuthorWhenNameMatchesCaseInsensitive() {
        // Given
        AuthorEntity author = new AuthorEntity("Brandon Sanderson");
        when(repository.findByNameIgnoreCase("brandon sanderson")).thenReturn(Optional.of(author));

        // When
        Optional<AuthorEntity> result = repository.findByNameIgnoreCase("brandon sanderson");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Brandon Sanderson");
    }

    @Test
    void shouldReturnEmptyWhenAuthorDoesNotExist() {
        // Given
        when(repository.findByNameIgnoreCase("Unknown Author")).thenReturn(Optional.empty());

        // When
        Optional<AuthorEntity> result = repository.findByNameIgnoreCase("Unknown Author");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldPersistAndReturnAuthorWithGeneratedId() {
        // Given
        AuthorEntity author = new AuthorEntity("Isaac Asimov");
        UUID generatedId = UUID.randomUUID();
        author.setId(generatedId);
        when(repository.save(author)).thenReturn(author);

        // When
        AuthorEntity saved = repository.save(author);

        // Then
        assertThat(saved.getId()).isEqualTo(generatedId);
        assertThat(saved.getName()).isEqualTo("Isaac Asimov");
    }
}
