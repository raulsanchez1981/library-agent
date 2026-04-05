package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.AuthorEntity;

import java.util.List;
import java.util.UUID;

public record AutorDetailDto(
        UUID id,
        String name,
        String photoUrl,
        String bio,
        String openLibraryOlid,
        List<AutorBookDto> books
) {
    public static AutorDetailDto fromEntity(AuthorEntity entity, List<AutorBookDto> books) {
        return new AutorDetailDto(
                entity.getId(),
                entity.getName(),
                entity.getPhotoUrl(),
                entity.getBio(),
                entity.getOpenLibraryOlid(),
                books
        );
    }
}
