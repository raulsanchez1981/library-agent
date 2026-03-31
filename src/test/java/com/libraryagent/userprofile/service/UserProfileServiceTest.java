package com.libraryagent.userprofile.service;

import com.libraryagent.shared.exception.LibraryAgentException;
import com.libraryagent.userprofile.dto.UpdateUserProfileRequest;
import com.libraryagent.userprofile.dto.UserProfileDto;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    UserProfileRepository repository;

    @InjectMocks
    UserProfileServiceImpl service;

    @Test
    void shouldReturnExistingProfileWhenEmailFound() {
        // Given
        UserProfile profile = profileWithEmail("test@example.com");
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(profile));

        // When
        UserProfileDto result = service.getOrCreateByEmail("test@example.com");

        // Then
        assertThat(result.email()).isEqualTo("test@example.com");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldCreateProfileWhenEmailNotFound() {
        // Given
        UserProfile saved = profileWithEmail("new@example.com");
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(saved);

        // When
        UserProfileDto result = service.getOrCreateByEmail("new@example.com");

        // Then
        assertThat(result.email()).isEqualTo("new@example.com");
        verify(repository).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenFindByIdMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(LibraryAgentException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        // Given
        UserProfile profile = profileWithEmail("user@example.com");
        UUID id = profile.getId();
        when(repository.findById(id)).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "en", null, List.of("Fantasy", "Sci-Fi"), null
        );

        // When
        UserProfileDto result = service.updatePreferences(id, request);

        // Then — idioma actualizado, threshold sin cambiar, géneros actualizados
        assertThat(result.preferredLanguage()).isEqualTo("en");
        assertThat(result.minScoreThreshold()).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(result.favoriteGenres()).containsExactly("Fantasy", "Sci-Fi");
    }

    @Test
    void shouldReplaceGenreListOnUpdate() {
        // Given
        UserProfile profile = profileWithEmail("user@example.com");
        profile.getFavoriteGenres().addAll(List.of("Horror", "Romance"));
        UUID id = profile.getId();
        when(repository.findById(id)).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null, null, List.of("Fantasy"), null
        );

        // When
        UserProfileDto result = service.updatePreferences(id, request);

        // Then — lista reemplazada, no acumulada
        assertThat(result.favoriteGenres()).containsExactly("Fantasy");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingProfile() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.updatePreferences(id, new UpdateUserProfileRequest(null, null, null, null)))
                .isInstanceOf(LibraryAgentException.class);
    }

    private static UserProfile profileWithEmail(String email) {
        UserProfile p = new UserProfile();
        p.setEmail(email);
        // Simulamos el ID que asignaría Hibernate
        try {
            var field = UserProfile.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }
}
