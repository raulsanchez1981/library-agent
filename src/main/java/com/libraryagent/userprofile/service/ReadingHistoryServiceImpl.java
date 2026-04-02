package com.libraryagent.userprofile.service;

import com.libraryagent.shared.exception.LibraryAgentException;
import com.libraryagent.userprofile.dto.AddReadingHistoryRequest;
import com.libraryagent.userprofile.dto.ReadingHistoryDto;
import com.libraryagent.userprofile.dto.UpdateReadingHistoryRequest;
import com.libraryagent.userprofile.model.ReadingHistoryEntity;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.ReadingHistoryRepository;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryRepository historyRepository;
    private final UserProfileRepository profileRepository;

    public ReadingHistoryServiceImpl(ReadingHistoryRepository historyRepository,
                                     UserProfileRepository profileRepository) {
        this.historyRepository = historyRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public List<ReadingHistoryDto> findAllByProfileId(UUID profileId) {
        return historyRepository.findAllByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(ReadingHistoryDto::from)
                .toList();
    }

    @Override
    @Transactional
    public ReadingHistoryDto add(UUID profileId, AddReadingHistoryRequest request) {
        UserProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> LibraryAgentException.notFound("Perfil no encontrado: " + profileId));

        ReadingHistoryEntity entry = new ReadingHistoryEntity();
        entry.setProfile(profile);
        entry.setBookTitle(request.bookTitle());
        entry.setBookAuthor(request.bookAuthor());
        entry.setStatus(request.status());
        entry.setStartedAt(request.startedAt());
        entry.setFinishedAt(request.finishedAt());
        entry.setRating(request.rating());
        entry.setNotes(request.notes());

        return ReadingHistoryDto.from(historyRepository.save(entry));
    }

    @Override
    @Transactional
    public ReadingHistoryDto update(UUID profileId, UUID historyId, UpdateReadingHistoryRequest request) {
        ReadingHistoryEntity entry = historyRepository.findByIdAndProfileId(historyId, profileId)
                .orElseThrow(() -> LibraryAgentException.notFound("Entrada de historial no encontrada: " + historyId));

        if (request.bookTitle() != null) {
            entry.setBookTitle(request.bookTitle());
        }
        if (request.bookAuthor() != null) {
            entry.setBookAuthor(request.bookAuthor());
        }
        if (request.status() != null) {
            entry.setStatus(request.status());
        }
        if (request.startedAt() != null) {
            entry.setStartedAt(request.startedAt());
        }
        if (request.finishedAt() != null) {
            entry.setFinishedAt(request.finishedAt());
        }
        if (request.rating() != null) {
            entry.setRating(request.rating());
        }
        if (request.notes() != null) {
            entry.setNotes(request.notes());
        }

        return ReadingHistoryDto.from(historyRepository.save(entry));
    }

    @Override
    @Transactional
    public void delete(UUID profileId, UUID historyId) {
        ReadingHistoryEntity entry = historyRepository.findByIdAndProfileId(historyId, profileId)
                .orElseThrow(() -> LibraryAgentException.notFound("Entrada de historial no encontrada: " + historyId));
        historyRepository.delete(entry);
    }
}
