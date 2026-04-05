package com.libraryagent.recommendation.service;

import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.recommendation.model.RecommendationDto;
import com.libraryagent.recommendation.model.RecommendationEntity;
import com.libraryagent.recommendation.model.RecommendationStatus;
import com.libraryagent.recommendation.model.ScoringResult;
import com.libraryagent.recommendation.model.UserPreferences;
import com.libraryagent.recommendation.repository.RecommendationRepository;
import com.libraryagent.recommendation.scoring.BookScoringStrategy;
import com.libraryagent.userprofile.model.ReadingStatus;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.ReadingHistoryRepository;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import com.libraryagent.shared.exception.LibraryAgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "recommendation:book:";
    private static final Duration REDIS_TTL = Duration.ofHours(24);
    private static final List<Confidence> SCOREABLE_CONFIDENCES = List.of(Confidence.HIGH, Confidence.MEDIUM);

    private final ExtractedBookRepository extractedBookRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final BookScoringStrategy scoringStrategy;
    private final StringRedisTemplate redisTemplate;

    @Value("${recommendation.user-email:}")
    private String configuredUserEmail;

    public RecommendationServiceImpl(
            ExtractedBookRepository extractedBookRepository,
            RecommendationRepository recommendationRepository,
            UserProfileRepository userProfileRepository,
            ReadingHistoryRepository readingHistoryRepository,
            BookScoringStrategy scoringStrategy,
            StringRedisTemplate redisTemplate) {
        this.extractedBookRepository = extractedBookRepository;
        this.recommendationRepository = recommendationRepository;
        this.userProfileRepository = userProfileRepository;
        this.readingHistoryRepository = readingHistoryRepository;
        this.scoringStrategy = scoringStrategy;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getRecommendations(Pageable pageable) {
        return recommendationRepository
                .findByStatusOrderByScoreDesc(RecommendationStatus.NUEVA, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public RecommendationDto dismiss(UUID id) {
        RecommendationEntity entity = recommendationRepository.findById(id)
                .orElseThrow(() -> LibraryAgentException.notFound("Recomendación no encontrada: " + id));
        entity.setStatus(RecommendationStatus.DESCARTADA);
        return toDto(recommendationRepository.save(entity));
    }

    @Override
    @Transactional
    public int scoreAndPersistPendingBooks(int maxBatch) {
        UserProfile userProfile = resolveUserProfile();
        UserPreferences preferences = buildPreferences(userProfile);

        List<ExtractedBookEntity> candidates = extractedBookRepository
                .findByEnrichedTrueAndConfidenceIn(SCOREABLE_CONFIDENCES)
                .stream()
                .filter(book -> !recommendationRepository.existsByExtractedBookId(book.getId()))
                .limit(maxBatch)
                .toList();

        int processed = 0;
        for (ExtractedBookEntity book : candidates) {
            try {
                ScoringResult result = scoreWithCache(book, preferences);
                persistRecommendation(book, result);
                processed++;
            } catch (Exception e) {
                log.error("Error al puntuar libro '{}' (id={}): {}", book.getTitle(), book.getId(), e.getMessage());
            }
        }

        log.info("Scoring completado: {}/{} libros procesados", processed, candidates.size());
        return processed;
    }

    private ScoringResult scoreWithCache(ExtractedBookEntity book, UserPreferences preferences) {
        String redisKey = REDIS_KEY_PREFIX + book.getId();
        String cached = redisTemplate.opsForValue().get(redisKey);

        if (cached != null) {
            log.debug("Score recuperado de caché para libro '{}'", book.getTitle());
            return new ScoringResult(Integer.parseInt(cached), "Score recuperado de caché");
        }

        ScoringResult result = scoringStrategy.score(book, preferences);

        redisTemplate.opsForValue().set(redisKey, String.valueOf(result.score()), REDIS_TTL);
        return result;
    }

    private void persistRecommendation(ExtractedBookEntity book, ScoringResult result) {
        RecommendationEntity entity = new RecommendationEntity();
        entity.setExtractedBook(book);
        entity.setScore(result.score());
        entity.setReasoning(result.reasoning());
        entity.setStatus(RecommendationStatus.NUEVA);
        entity.setScoredAt(Instant.now());
        recommendationRepository.save(entity);
    }

    private UserProfile resolveUserProfile() {
        if (configuredUserEmail != null && !configuredUserEmail.isBlank()) {
            return userProfileRepository.findByEmail(configuredUserEmail)
                    .orElseGet(() -> userProfileRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("No hay perfil de usuario configurado")));
        }
        return userProfileRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay perfil de usuario configurado"));
    }

    private UserPreferences buildPreferences(UserProfile profile) {
        List<String> completedTitles = readingHistoryRepository
                .findAllByProfileIdAndStatus(profile.getId(), ReadingStatus.READ)
                .stream()
                .map(entry -> entry.getBookTitle())
                .toList();

        int minScore = profile.getMinScoreThreshold()
                .multiply(java.math.BigDecimal.valueOf(100))
                .intValue();

        return new UserPreferences(
                profile.getFavoriteGenres(),
                profile.getFavoriteAuthors(),
                profile.getPreferredLanguage(),
                minScore,
                completedTitles
        );
    }

    private RecommendationDto toDto(RecommendationEntity entity) {
        ExtractedBookEntity book = entity.getExtractedBook();
        String title = book.getTitleEs() != null && !book.getTitleEs().isBlank()
                ? book.getTitleEs()
                : book.getTitle();
        String author = book.getAuthorCorrected() != null && !book.getAuthorCorrected().isBlank()
                ? book.getAuthorCorrected()
                : book.getAuthor();

        return new RecommendationDto(
                entity.getId(),
                book.getId(),
                title,
                author,
                entity.getScore(),
                entity.getReasoning(),
                entity.getStatus(),
                entity.getScoredAt()
        );
    }
}
