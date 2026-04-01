package com.libraryagent.recommendation.model;

import java.time.Instant;
import java.util.UUID;

public record RecommendationDto(
        UUID id,
        UUID extractedBookId,
        String bookTitle,
        String bookAuthor,
        int score,
        String reasoning,
        RecommendationStatus status,
        Instant scoredAt
) {}
