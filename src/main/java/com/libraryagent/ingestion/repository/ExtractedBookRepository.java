package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExtractedBookRepository extends JpaRepository<ExtractedBookEntity, UUID> {

    List<ExtractedBookEntity> findBySourceMentionId(UUID sourceMentionId);

    List<ExtractedBookEntity> findByEnrichedFalse();

    boolean existsByTitleIgnoreCase(String title);

    long countByEnrichedFalse();

    List<ExtractedBookEntity> findByEnrichedTrueAndAuthorIsNull();

    List<ExtractedBookEntity> findByEnrichedTrueAndConfidenceIn(List<Confidence> confidences);
}
