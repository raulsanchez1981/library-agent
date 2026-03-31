package com.libraryagent.userprofile.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateUserProfileRequest(
        @Size(min = 2, max = 10)
        String preferredLanguage,

        @DecimalMin("0.00") @DecimalMax("1.00")
        BigDecimal minScoreThreshold,

        List<@Size(max = 100) String> favoriteGenres,

        List<@Size(max = 256) String> favoriteAuthors
) {}
