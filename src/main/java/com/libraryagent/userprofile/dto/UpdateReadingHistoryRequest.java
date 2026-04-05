package com.libraryagent.userprofile.dto;

import com.libraryagent.userprofile.model.ReadingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateReadingHistoryRequest(
        @Size(max = 512)
        String bookTitle,

        @Size(max = 256)
        String bookAuthor,

        ReadingStatus status,
        LocalDate startedAt,
        LocalDate finishedAt,
        @Min(1) @Max(5) Short rating,
        String notes
) {}
