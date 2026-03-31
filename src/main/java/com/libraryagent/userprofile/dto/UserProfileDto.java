package com.libraryagent.userprofile.dto;

import com.libraryagent.userprofile.model.UserProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String email,
        String preferredLanguage,
        BigDecimal minScoreThreshold,
        List<String> favoriteGenres,
        List<String> favoriteAuthors,
        Instant createdAt
) {
    public static UserProfileDto from(UserProfile entity) {
        return new UserProfileDto(
                entity.getId(),
                entity.getEmail(),
                entity.getPreferredLanguage(),
                entity.getMinScoreThreshold(),
                List.copyOf(entity.getFavoriteGenres()),
                List.copyOf(entity.getFavoriteAuthors()),
                entity.getCreatedAt()
        );
    }
}
