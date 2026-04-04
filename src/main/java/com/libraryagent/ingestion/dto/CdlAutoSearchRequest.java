package com.libraryagent.ingestion.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CdlAutoSearchRequest(
        @NotEmpty List<UUID> verifiedTitleIds
) {}
