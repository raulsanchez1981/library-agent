package com.libraryagent.userprofile.repository;

import com.libraryagent.userprofile.model.ReadingHistoryEntity;
import com.libraryagent.userprofile.model.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEntity, UUID> {

    List<ReadingHistoryEntity> findAllByProfileIdOrderByCreatedAtDesc(UUID profileId);

    List<ReadingHistoryEntity> findAllByProfileIdAndStatus(UUID profileId, ReadingStatus status);

    Optional<ReadingHistoryEntity> findByIdAndProfileId(UUID id, UUID profileId);
}
