package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;

import java.util.UUID;

public record AuthorBookDto(
        UUID id,
        String name,
        String coverUrl
) {
    public static AuthorBookDto fromEntity(VerifiedTitleEntity entity) {
        return new AuthorBookDto(entity.getId(), entity.getName(), entity.getCoverUrl());
    }
}
