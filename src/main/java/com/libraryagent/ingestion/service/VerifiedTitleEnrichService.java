package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;

import java.util.UUID;

public interface VerifiedTitleEnrichService {

    VerifiedTitleDetailDto enrichFromCdl(UUID id, String casaDelLibroUrl);
}
