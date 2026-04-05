package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.service.CdlAutoSearchStatus;

import java.util.List;
import java.util.UUID;

public record VerifiedTitleDto(
        UUID id,
        String name,
        List<String> authors,
        String coverUrl,
        String synopsis,
        String googleBooksId,
        boolean cdlEnriched,
        CdlAutoSearchStatus cdlAutoSearchStatus,
        String publisher,
        String publishedDate,
        Integer pageCount,
        String isbn
) {

    public static VerifiedTitleDto fromEntity(VerifiedTitleEntity entity, List<String> authors) {
        return new VerifiedTitleDto(
                entity.getId(),
                entity.getName(),
                authors,
                entity.getCoverUrl(),
                entity.getSynopsis(),
                entity.getGoogleBooksId(),
                entity.getCasaDelLibroUrl() != null,
                entity.getCdlAutoSearchStatus(),
                entity.getPublisher(),
                entity.getPublishedDate(),
                entity.getPageCount(),
                entity.getIsbn()
        );
    }
}
