package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.AuthorEntity;

import java.util.UUID;

public record AuthorSummaryDto(
        UUID id,
        String name,
        String photoUrl,
        int bookCount
) {
    public static AuthorSummaryDto fromEntity(AuthorEntity entity, int bookCount) {
        return new AuthorSummaryDto(entity.getId(), entity.getName(), entity.getPhotoUrl(), bookCount);
    }
}
