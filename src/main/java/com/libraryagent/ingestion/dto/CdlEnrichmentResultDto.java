package com.libraryagent.ingestion.dto;

import java.util.List;

public record CdlEnrichmentResultDto(
        String coverUrl,
        String synopsis,
        String technicalSheet,
        List<String> genres
) {}
