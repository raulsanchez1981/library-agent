package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {

    Optional<AuthorEntity> findByNameIgnoreCase(String name);

    List<AuthorEntity> findByVerifiedTrue();
}
