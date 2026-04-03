package com.libraryagent.ingestion.dto;

import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.model.ExtractedBookEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtractedBookAdminDto(
        UUID id,
        String title,
        String author,
        boolean isSaga,
        String titleEs,
        String titleEsOl,
        String authorCorrected,
        boolean availableInSpanish,
        EnrichmentSource enrichmentSource,
        Confidence confidence,
        boolean enriched,
        Instant enrichedAt,
        Instant createdAt,
        List<String> authors,
        UUID verifiedTitleId,
        String verifiedTitleName,
        String coverUrl,
        String synopsis,
        boolean cdlEnriched
) {

    public static ExtractedBookAdminDto fromEntity(ExtractedBookEntity e) {
        List<String> authorNames = e.getAuthors().stream()
                .map(AuthorEntity::getName)
                .toList();
        VerifiedTitleEntity vt = e.getVerifiedTitle();
        UUID verifiedTitleId = vt != null ? vt.getId() : null;
        String verifiedTitleName = vt != null ? vt.getName() : null;
        String coverUrl = vt != null ? vt.getCoverUrl() : null;
        String synopsis = vt != null ? vt.getSynopsis() : null;
        boolean cdlEnriched = vt != null && vt.getCasaDelLibroUrl() != null;
        return new ExtractedBookAdminDto(
                e.getId(),
                e.getTitle(),
                e.getAuthor(),
                e.isSaga(),
                e.getTitleEs(),
                e.getTitleEsOl(),
                e.getAuthorCorrected(),
                e.isAvailableInSpanish(),
                e.getEnrichmentSource(),
                e.getConfidence(),
                e.isEnriched(),
                e.getEnrichedAt(),
                e.getCreatedAt(),
                authorNames,
                verifiedTitleId,
                verifiedTitleName,
                coverUrl,
                synopsis,
                cdlEnriched
        );
    }
}
