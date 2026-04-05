package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.AuthorEntity;

import java.util.UUID;

public record AuthorRefDto(
        UUID id,
        String name
) {
    public static AuthorRefDto fromEntity(AuthorEntity entity) {
        return new AuthorRefDto(entity.getId(), entity.getName());
    }
}
