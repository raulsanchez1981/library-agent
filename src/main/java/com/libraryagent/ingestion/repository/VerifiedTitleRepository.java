package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerifiedTitleRepository extends JpaRepository<VerifiedTitleEntity, UUID> {

    Optional<VerifiedTitleEntity> findByNameIgnoreCase(String name);

    List<VerifiedTitleEntity> findAllByOrderByNameAsc();

    @Query("SELECT vt FROM VerifiedTitleEntity vt LEFT JOIN FETCH vt.genres WHERE vt.id = :id")
    Optional<VerifiedTitleEntity> findByIdWithGenres(@Param("id") UUID id);
}
