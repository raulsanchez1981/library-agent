package com.libraryagent.userprofile.service;

import com.libraryagent.shared.exception.LibraryAgentException;
import com.libraryagent.userprofile.dto.AddReadingHistoryRequest;
import com.libraryagent.userprofile.dto.ReadingHistoryDto;
import com.libraryagent.userprofile.dto.UpdateReadingHistoryRequest;
import com.libraryagent.userprofile.model.ReadingHistoryEntity;
import com.libraryagent.userprofile.model.ReadingStatus;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.ReadingHistoryRepository;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingHistoryServiceTest {

    @Mock
    ReadingHistoryRepository historyRepository;

    @Mock
    UserProfileRepository profileRepository;

    @InjectMocks
    ReadingHistoryServiceImpl service;

    @Test
    void shouldReturnHistoryOrderedByCreatedAtDesc() {
        // Given
        UUID profileId = UUID.randomUUID();
        ReadingHistoryEntity e1 = entryWithTitle("Dune");
        ReadingHistoryEntity e2 = entryWithTitle("Foundation");
        when(historyRepository.findAllByProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(List.of(e1, e2));

        // When
        List<ReadingHistoryDto> result = service.findAllByProfileId(profileId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).bookTitle()).isEqualTo("Dune");
        assertThat(result.get(1).bookTitle()).isEqualTo("Foundation");
    }

    @Test
    void shouldReturnEmptyListWhenNoHistoryExists() {
        // Given
        UUID profileId = UUID.randomUUID();
        when(historyRepository.findAllByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of());

        // When
        List<ReadingHistoryDto> result = service.findAllByProfileId(profileId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldAddEntryWithAllFields() {
        // Given
        UUID profileId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        ReadingHistoryEntity saved = entryWithTitle("Mistborn");
        saved.setStatus(ReadingStatus.READ);
        saved.setRating((short) 5);
        when(historyRepository.save(any())).thenReturn(saved);

        AddReadingHistoryRequest request = new AddReadingHistoryRequest(
                "Mistborn", "Brandon Sanderson", ReadingStatus.READ,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20),
                (short) 5, "Excelente"
        );

        // When
        ReadingHistoryDto result = service.add(profileId, request);

        // Then
        assertThat(result.bookTitle()).isEqualTo("Mistborn");
        assertThat(result.rating()).isEqualTo((short) 5);
        verify(historyRepository).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenAddingToMissingProfile() {
        // Given
        UUID profileId = UUID.randomUUID();
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        AddReadingHistoryRequest request = new AddReadingHistoryRequest(
                "Dune", null, ReadingStatus.PENDING, null, null, null, null
        );

        // When / Then
        assertThatThrownBy(() -> service.add(profileId, request))
                .isInstanceOf(LibraryAgentException.class)
                .hasMessageContaining(profileId.toString());
    }

    @Test
    void shouldUpdateOnlyStatusWhenOtherFieldsNull() {
        // Given
        UUID profileId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();

        ReadingHistoryEntity existing = entryWithTitle("The Name of the Wind");
        existing.setStatus(ReadingStatus.PENDING);
        existing.setRating(null);

        when(historyRepository.findByIdAndProfileId(historyId, profileId)).thenReturn(Optional.of(existing));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateReadingHistoryRequest request = new UpdateReadingHistoryRequest(
                ReadingStatus.IN_PROGRESS, null, null, null, null
        );

        // When
        ReadingHistoryDto result = service.update(profileId, historyId, request);

        // Then
        assertThat(result.status()).isEqualTo(ReadingStatus.IN_PROGRESS);
        assertThat(result.rating()).isNull();
    }

    @Test
    void shouldThrowNotFoundWhenEntryDoesNotBelongToProfile() {
        // Given
        UUID profileId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();
        when(historyRepository.findByIdAndProfileId(historyId, profileId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.update(profileId, historyId,
                new UpdateReadingHistoryRequest(null, null, null, null, null)))
                .isInstanceOf(LibraryAgentException.class);
    }

    private static ReadingHistoryEntity entryWithTitle(String title) {
        ReadingHistoryEntity e = new ReadingHistoryEntity();
        e.setBookTitle(title);
        e.setStatus(ReadingStatus.PENDING);
        return e;
    }
}
