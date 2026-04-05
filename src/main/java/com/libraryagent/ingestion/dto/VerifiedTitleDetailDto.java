package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.service.CdlAutoSearchStatus;

import java.util.List;
import java.util.UUID;


public record VerifiedTitleDetailDto(
        UUID id,
        String name,
        String coverUrl,
        String synopsis,
        String technicalSheet,
        String casaDelLibroUrl,
        List<GenreDto> genres,
        List<AuthorRefDto> authors,
        CdlAutoSearchStatus cdlAutoSearchStatus,
        String publisher,
        String publishedDate,
        Integer pageCount,
        String isbn
) {

    public static VerifiedTitleDetailDto fromEntity(VerifiedTitleEntity entity, List<AuthorRefDto> authors) {
        List<GenreDto> genreDtos = entity.getGenres().stream()
                .map(GenreDto::fromEntity)
                .toList();

        return new VerifiedTitleDetailDto(
                entity.getId(),
                entity.getName(),
                entity.getCoverUrl(),
                entity.getSynopsis(),
                entity.getTechnicalSheet(),
                entity.getCasaDelLibroUrl(),
                genreDtos,
                authors,
                entity.getCdlAutoSearchStatus(),
                entity.getPublisher(),
                entity.getPublishedDate(),
                entity.getPageCount(),
                entity.getIsbn()
        );
    }
}
