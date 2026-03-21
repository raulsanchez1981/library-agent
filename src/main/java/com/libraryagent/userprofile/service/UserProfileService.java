package com.libraryagent.userprofile.service;

import com.libraryagent.shared.exception.LibraryAgentException;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> LibraryAgentException.notFound("Perfil no encontrado: " + id));
    }

    public UserProfile findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> LibraryAgentException.notFound("Perfil no encontrado para email"));
    }

    @Transactional
    public UserProfile create(String email) {
        if (repository.existsByEmail(email)) {
            throw LibraryAgentException.badRequest("Ya existe un perfil con ese email");
        }
        UserProfile profile = new UserProfile();
        profile.setEmail(email);
        return repository.save(profile);
    }
}
