package com.libraryagent.userprofile.controller;

import com.libraryagent.shared.model.ApiResponse;
import com.libraryagent.userprofile.dto.UpdateUserProfileRequest;
import com.libraryagent.userprofile.dto.UserProfileDto;
import com.libraryagent.userprofile.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfileDto profile = userProfileService.getOrCreateByEmail(jwt.getClaimAsString("email"));
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserProfileDto profile = userProfileService.getOrCreateByEmail(jwt.getClaimAsString("email"));
        UserProfileDto updated = userProfileService.updatePreferences(profile.id(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}
