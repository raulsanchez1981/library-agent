package com.libraryagent.ingestion.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGenreRequest(
        @NotBlank String name
) {}
