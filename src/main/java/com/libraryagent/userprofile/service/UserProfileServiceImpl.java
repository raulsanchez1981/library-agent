package com.libraryagent.userprofile.service;

import com.libraryagent.shared.exception.LibraryAgentException;
import com.libraryagent.userprofile.dto.UpdateUserProfileRequest;
import com.libraryagent.userprofile.dto.UserProfileDto;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileServiceImpl(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserProfileDto findById(UUID id) {
        return repository.findById(id)
                .map(UserProfileDto::from)
                .orElseThrow(() -> LibraryAgentException.notFound("Perfil no encontrado: " + id));
    }

    @Override
    @Transactional
    public UserProfileDto getOrCreateByEmail(String email) {
        return repository.findByEmail(email)
                .map(UserProfileDto::from)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setEmail(email);
                    return UserProfileDto.from(repository.save(profile));
                });
    }

    @Override
    @Transactional
    public UserProfileDto updatePreferences(UUID id, UpdateUserProfileRequest request) {
        UserProfile profile = repository.findById(id)
                .orElseThrow(() -> LibraryAgentException.notFound("Perfil no encontrado: " + id));

        if (request.preferredLanguage() != null) {
            profile.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.minScoreThreshold() != null) {
            profile.setMinScoreThreshold(request.minScoreThreshold());
        }
        if (request.favoriteGenres() != null) {
            profile.getFavoriteGenres().clear();
            profile.getFavoriteGenres().addAll(request.favoriteGenres());
        }
        if (request.favoriteAuthors() != null) {
            profile.getFavoriteAuthors().clear();
            profile.getFavoriteAuthors().addAll(request.favoriteAuthors());
        }

        return UserProfileDto.from(repository.save(profile));
    }
}
