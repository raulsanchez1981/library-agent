package com.libraryagent.ingestion.dto;

public record UpdateExtractedBookRequest(
        String titleEs,
        String authorCorrected,
        Boolean availableInSpanish,
        Boolean isSaga
) {}
