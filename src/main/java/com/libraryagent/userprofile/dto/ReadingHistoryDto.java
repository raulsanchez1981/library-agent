package com.libraryagent.userprofile.dto;

import com.libraryagent.userprofile.model.ReadingHistoryEntity;
import com.libraryagent.userprofile.model.ReadingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReadingHistoryDto(
        UUID id,
        String bookTitle,
        String bookAuthor,
        ReadingStatus status,
        LocalDate startedAt,
        LocalDate finishedAt,
        Short rating,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReadingHistoryDto from(ReadingHistoryEntity entity) {
        return new ReadingHistoryDto(
                entity.getId(),
                entity.getBookTitle(),
                entity.getBookAuthor(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getRating(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
