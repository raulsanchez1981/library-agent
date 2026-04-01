package com.libraryagent.recommendation.scoring;

import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.recommendation.model.ScoringResult;
import com.libraryagent.recommendation.model.UserPreferences;

public interface BookScoringStrategy {

    ScoringResult score(ExtractedBookEntity book, UserPreferences preferences);
}
