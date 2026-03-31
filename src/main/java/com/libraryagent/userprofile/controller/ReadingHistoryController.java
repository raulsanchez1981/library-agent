package com.libraryagent.userprofile.controller;

import com.libraryagent.shared.model.ApiResponse;
import com.libraryagent.userprofile.dto.AddReadingHistoryRequest;
import com.libraryagent.userprofile.dto.ReadingHistoryDto;
import com.libraryagent.userprofile.dto.UpdateReadingHistoryRequest;
import com.libraryagent.userprofile.service.ReadingHistoryService;
import com.libraryagent.userprofile.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reading-history")
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;
    private final UserProfileService userProfileService;

    public ReadingHistoryController(ReadingHistoryService readingHistoryService,
                                    UserProfileService userProfileService) {
        this.readingHistoryService = readingHistoryService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReadingHistoryDto>>> getHistory(@AuthenticationPrincipal Jwt jwt) {
        UUID profileId = resolveProfileId(jwt);
        List<ReadingHistoryDto> history = readingHistoryService.findAllByProfileId(profileId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReadingHistoryDto>> addEntry(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddReadingHistoryRequest request) {
        UUID profileId = resolveProfileId(jwt);
        ReadingHistoryDto created = readingHistoryService.add(profileId, request);
        URI location = URI.create("/api/v1/reading-history/" + created.id());
        return ResponseEntity.created(location).body(ApiResponse.ok(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ReadingHistoryDto>> updateEntry(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReadingHistoryRequest request) {
        UUID profileId = resolveProfileId(jwt);
        ReadingHistoryDto updated = readingHistoryService.update(profileId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    private UUID resolveProfileId(Jwt jwt) {
        return userProfileService.getOrCreateByEmail(jwt.getClaimAsString("email")).id();
    }
}
