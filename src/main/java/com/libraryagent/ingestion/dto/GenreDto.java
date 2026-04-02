package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.GenreEntity;

import java.util.UUID;

public record GenreDto(
        UUID id,
        String name
) {

    public static GenreDto fromEntity(GenreEntity entity) {
        return new GenreDto(entity.getId(), entity.getName());
    }
}
