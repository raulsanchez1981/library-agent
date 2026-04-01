package com.libraryagent.recommendation.repository;

import com.libraryagent.recommendation.model.RecommendationEntity;
import com.libraryagent.recommendation.model.RecommendationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<RecommendationEntity, UUID> {

    Page<RecommendationEntity> findByStatusOrderByScoreDesc(RecommendationStatus status, Pageable pageable);

    Page<RecommendationEntity> findAllByOrderByScoreDesc(Pageable pageable);

    boolean existsByExtractedBookId(UUID extractedBookId);
}
