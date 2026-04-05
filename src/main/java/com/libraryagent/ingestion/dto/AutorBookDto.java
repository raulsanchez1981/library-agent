package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;

import java.util.UUID;

public record AutorBookDto(
        UUID id,
        String name,
        String coverUrl
) {
    public static AutorBookDto fromEntity(VerifiedTitleEntity entity) {
        return new AutorBookDto(entity.getId(), entity.getName(), entity.getCoverUrl());
    }
}
