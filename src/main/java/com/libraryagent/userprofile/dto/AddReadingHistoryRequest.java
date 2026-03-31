package com.libraryagent.userprofile.dto;

import com.libraryagent.userprofile.model.ReadingStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record AddReadingHistoryRequest(
        @NotBlank @Size(max = 512)
        String bookTitle,

        @Size(max = 256)
        String bookAuthor,

        @NotNull
        ReadingStatus status,

        LocalDate startedAt,

        LocalDate finishedAt,

        @Min(1) @Max(5)
        Short rating,

        String notes
) {}
