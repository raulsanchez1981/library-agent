package com.libraryagent.recommendation.repository;

import com.libraryagent.recommendation.model.BookScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BookScoreRepository extends JpaRepository<BookScoreEntity, UUID> {

    List<BookScoreEntity> findByScoreGreaterThanEqualOrderByScoreDesc(BigDecimal minScore);

    boolean existsByBookTitleIgnoreCase(String bookTitle);
}
