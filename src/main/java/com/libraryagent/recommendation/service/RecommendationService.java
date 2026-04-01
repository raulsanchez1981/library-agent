package com.libraryagent.recommendation.service;

import com.libraryagent.recommendation.model.RecommendationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecommendationService {

    Page<RecommendationDto> getRecommendations(Pageable pageable);

    RecommendationDto dismiss(UUID id);

    /**
     * Puntúa los libros pendientes y persiste las recomendaciones.
     * @return número de libros procesados
     */
    int scoreAndPersistPendingBooks(int maxBatch);
}
