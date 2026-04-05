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

    /** Títulos sin búsqueda CDL intentada y sin URL de Casa del Libro ya asignada */
    @Query("SELECT vt.id FROM VerifiedTitleEntity vt WHERE vt.cdlAutoSearchStatus IS NULL AND vt.casaDelLibroUrl IS NULL")
    List<UUID> findAllPendingCdlSearch();

    /** Títulos enriquecidos automáticamente por Google Books (sin CDL) — para re-enriquecimiento forzado */
    @Query("SELECT vt.id FROM VerifiedTitleEntity vt WHERE vt.cdlAutoSearchStatus = 'AUTO' AND vt.casaDelLibroUrl IS NULL")
    List<UUID> findAllAutoEnrichedWithoutCdl();

    /** Títulos sin ningún género asignado */
    @Query("SELECT vt FROM VerifiedTitleEntity vt LEFT JOIN FETCH vt.genres WHERE vt.genres IS EMPTY")
    List<VerifiedTitleEntity> findAllWithoutGenres();

    /** Todos los títulos con sus géneros cargados (para enrichAllGenres) */
    @Query("SELECT vt FROM VerifiedTitleEntity vt LEFT JOIN FETCH vt.genres")
    List<VerifiedTitleEntity> findAllWithGenres();

    /** Títulos verificados de un autor concreto, ordenados por nombre */
    @Query(value = """
            SELECT DISTINCT vt.* FROM verified_titles vt
            JOIN extracted_books eb ON eb.verified_title_id = vt.id
            JOIN book_authors ba ON ba.book_id = eb.id
            WHERE ba.author_id = :authorId
            AND eb.confidence = 'VERIFIED'
            ORDER BY vt.name
            """, nativeQuery = true)
    List<VerifiedTitleEntity> findAllByAuthorId(@Param("authorId") UUID authorId);

}
