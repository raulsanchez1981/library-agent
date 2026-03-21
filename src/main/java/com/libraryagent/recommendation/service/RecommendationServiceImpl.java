package com.libraryagent.recommendation.service;

import com.libraryagent.ingestion.model.ExtractedBook;
import com.libraryagent.recommendation.model.BookScore;
import com.libraryagent.recommendation.model.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    @Value("${recommendation.score-threshold:0.75}")
    private double scoreThreshold;

    @Value("${recommendation.claude-model:claude-opus-4-6}")
    private String claudeModel;

    @Override
    public BookScore score(ExtractedBook book, UserPreferences preferences) {
        // TODO: construir prompt desde src/main/resources/prompts/recommendation.txt
        // TODO: llamar a Claude API y deserializar respuesta JSON
        // TODO: persistir el resultado en book_scores
        log.warn("RecommendationService no implementado aún para libro: {}", book.title());

        return new BookScore(
                UUID.randomUUID(),
                book.title(),
                book.author(),
                0.0,
                List.of(),
                Instant.now()
        );
    }

    @Override
    public List<BookScore> recommend(List<ExtractedBook> books, UserPreferences preferences) {
        return books.stream()
                .map(book -> score(book, preferences))
                .filter(bookScore -> bookScore.meetsThreshold(scoreThreshold))
                .sorted(Comparator.comparingDouble(BookScore::score).reversed())
                .toList();
    }
}
