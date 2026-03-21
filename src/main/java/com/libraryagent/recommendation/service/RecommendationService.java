package com.libraryagent.recommendation.service;

import com.libraryagent.ingestion.model.ExtractedBook;
import com.libraryagent.recommendation.model.BookScore;
import com.libraryagent.recommendation.model.UserPreferences;

import java.util.List;

public interface RecommendationService {

    /**
     * Puntúa un libro contra las preferencias del usuario.
     * Llama a Claude API internamente para el análisis.
     */
    BookScore score(ExtractedBook book, UserPreferences preferences);

    /**
     * Filtra y ordena los libros que superan el umbral de score configurado.
     */
    List<BookScore> recommend(List<ExtractedBook> books, UserPreferences preferences);
}
