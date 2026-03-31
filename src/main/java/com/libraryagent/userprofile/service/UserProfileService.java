package com.libraryagent.userprofile.service;

import com.libraryagent.userprofile.dto.UpdateUserProfileRequest;
import com.libraryagent.userprofile.dto.UserProfileDto;

import java.util.UUID;

public interface UserProfileService {

    UserProfileDto findById(UUID id);

    UserProfileDto getOrCreateByEmail(String email);

    UserProfileDto updatePreferences(UUID id, UpdateUserProfileRequest request);
}
