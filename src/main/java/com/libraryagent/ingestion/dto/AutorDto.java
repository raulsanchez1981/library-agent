package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.AuthorEntity;

import java.util.UUID;

public record AutorDto(
        UUID id,
        String name,
        String photoUrl,
        int bookCount
) {
    public static AutorDto fromEntity(AuthorEntity entity, int bookCount) {
        return new AutorDto(entity.getId(), entity.getName(), entity.getPhotoUrl(), bookCount);
    }
}
