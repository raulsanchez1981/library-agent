package com.libraryagent.ingestion.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrichCdlRequest(
        @NotBlank String casaDelLibroUrl
) {}
