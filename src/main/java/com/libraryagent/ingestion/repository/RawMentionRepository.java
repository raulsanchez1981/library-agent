package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.model.RawMentionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RawMentionRepository extends JpaRepository<RawMentionEntity, UUID> {

    List<RawMentionEntity> findBySource(String source);

    List<RawMentionEntity> findByFetchedAtAfter(Instant since);

    boolean existsBySourceAndUrl(String source, String url);

    boolean existsByUrl(String url);
}
