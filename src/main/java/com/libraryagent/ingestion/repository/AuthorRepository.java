package com.libraryagent.ingestion.repository;

import com.libraryagent.ingestion.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {

    Optional<AuthorEntity> findByNameIgnoreCase(String name);

    List<AuthorEntity> findByVerifiedTrue();

    @Query(value = """
            SELECT DISTINCT a.* FROM authors a
            JOIN book_authors ba ON ba.author_id = a.id
            JOIN extracted_books eb ON eb.id = ba.book_id
            WHERE eb.verified_title_id IS NOT NULL
            AND eb.confidence = 'VERIFIED'
            ORDER BY a.name
            """, nativeQuery = true)
    List<AuthorEntity> findAllWithVerifiedBooks();
}
