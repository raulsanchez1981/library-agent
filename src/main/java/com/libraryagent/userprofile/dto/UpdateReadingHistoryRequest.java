package com.libraryagent.userprofile.dto;

import com.libraryagent.userprofile.model.ReadingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record UpdateReadingHistoryRequest(
        ReadingStatus status,
        LocalDate startedAt,
        LocalDate finishedAt,
        @Min(1) @Max(5) Short rating,
        String notes
) {}
