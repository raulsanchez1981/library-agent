package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.GenreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<GenreEntity, UUID> {

    Optional<GenreEntity> findByNameIgnoreCase(String name);
}
