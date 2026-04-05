package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExtractedBookRepository extends JpaRepository<ExtractedBookEntity, UUID>,
        JpaSpecificationExecutor<ExtractedBookEntity> {

    List<ExtractedBookEntity> findBySourceMentionId(UUID sourceMentionId);

    List<ExtractedBookEntity> findByEnrichedFalse();

    boolean existsByTitleIgnoreCase(String title);

    long countByEnrichedFalse();

    List<ExtractedBookEntity> findByEnrichedTrueAndAuthorIsNull();

    List<ExtractedBookEntity> findByEnrichedTrueAndConfidenceIn(List<Confidence> confidences);

    List<ExtractedBookEntity> findByVerifiedTitleIsNullAndTitleEsIsNotNull();

    List<ExtractedBookEntity> findByConfidenceNotAndAuthorCorrectedIsNotNull(Confidence confidence);

    List<ExtractedBookEntity> findByVerifiedTitleAndConfidence(VerifiedTitleEntity verifiedTitle, Confidence confidence);

    @Query("SELECT eb.verifiedTitle.id, a.name FROM ExtractedBookEntity eb " +
           "JOIN eb.authors a " +
           "WHERE eb.confidence = :confidence AND eb.verifiedTitle IS NOT NULL")
    List<Object[]> findVerifiedTitleIdAndAuthorNameByConfidence(@Param("confidence") Confidence confidence);

    @Query("SELECT e FROM ExtractedBookEntity e WHERE e.verifiedTitle.id = :vtId " +
           "AND e.titleEs IS NOT NULL " +
           "AND (e.authorCorrected IS NOT NULL OR e.author IS NOT NULL) " +
           "ORDER BY e.confidence DESC")
    List<ExtractedBookEntity> findByVerifiedTitleIdWithSearchData(@Param("vtId") UUID vtId, Pageable pageable);

    @Query("SELECT e FROM ExtractedBookEntity e WHERE " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(e.titleEs) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(e.titleEsOl) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY e.createdAt DESC")
    List<ExtractedBookEntity> searchByTitle(@Param("q") String q, Pageable pageable);
}
