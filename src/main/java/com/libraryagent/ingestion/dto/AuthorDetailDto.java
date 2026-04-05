package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.AuthorEntity;

import java.util.List;
import java.util.UUID;

public record AuthorDetailDto(
        UUID id,
        String name,
        String photoUrl,
        String bio,
        String openLibraryOlid,
        List<AuthorBookDto> books
) {
    public static AuthorDetailDto fromEntity(AuthorEntity entity, List<AuthorBookDto> books) {
        return new AuthorDetailDto(
                entity.getId(),
                entity.getName(),
                entity.getPhotoUrl(),
                entity.getBio(),
                entity.getOpenLibraryOlid(),
                books
        );
    }
}
