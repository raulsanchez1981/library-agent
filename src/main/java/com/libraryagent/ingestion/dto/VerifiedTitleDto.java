package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;

import java.util.List;
import java.util.UUID;

public record VerifiedTitleDto(
        UUID id,
        String name,
        List<String> authors,
        String coverUrl,
        String synopsis,
        String googleBooksId
) {

    public static VerifiedTitleDto fromEntity(VerifiedTitleEntity entity, List<String> authors) {
        return new VerifiedTitleDto(
                entity.getId(),
                entity.getName(),
                authors,
                entity.getCoverUrl(),
                entity.getSynopsis(),
                entity.getGoogleBooksId()
        );
    }
}
