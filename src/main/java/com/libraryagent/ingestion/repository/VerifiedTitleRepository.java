package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerifiedTitleRepository extends JpaRepository<VerifiedTitleEntity, UUID> {

    Optional<VerifiedTitleEntity> findByNameIgnoreCase(String name);

    List<VerifiedTitleEntity> findAllByOrderByNameAsc();
}
