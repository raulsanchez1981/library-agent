package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.model.ExtractedBookEntity;

import java.util.UUID;

public record BookSearchResultDto(
        UUID id,
        String title,
        String titleEs,
        String author
) {

    public static BookSearchResultDto fromEntity(ExtractedBookEntity entity) {
        String resolvedAuthor = entity.getAuthorCorrected() != null
                ? entity.getAuthorCorrected()
                : entity.getAuthor();
        return new BookSearchResultDto(
                entity.getId(),
                entity.getTitle(),
                entity.getTitleEs(),
                resolvedAuthor
        );
    }
}
