package com.libraryagent.recommendation;

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
import com.libraryagent.recommendation.service.RecommendationServiceImpl;
import com.libraryagent.userprofile.model.ReadingStatus;
import com.libraryagent.userprofile.model.UserProfile;
import com.libraryagent.userprofile.repository.ReadingHistoryRepository;
import com.libraryagent.userprofile.repository.UserProfileRepository;
import com.libraryagent.shared.exception.LibraryAgentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ExtractedBookRepository extractedBookRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ReadingHistoryRepository readingHistoryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    // Sealed interfaces no son mockeables; se usa un spy de la implementación concreta
    private SpyScoringStrategy spyScoringStrategy;

    private RecommendationServiceImpl service;

    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        spyScoringStrategy = new SpyScoringStrategy();

        service = new RecommendationServiceImpl(
                extractedBookRepository,
                recommendationRepository,
                userProfileRepository,
                readingHistoryRepository,
                spyScoringStrategy,
                redisTemplate
        );

        userProfile = new UserProfile();
        userProfile.setId(UUID.randomUUID());
        userProfile.setEmail("test@example.com");
        userProfile.setPreferredLanguage("es");
        userProfile.setMinScoreThreshold(new BigDecimal("0.75"));
        userProfile.setFavoriteGenres(List.of("fantasía"));
        userProfile.setFavoriteAuthors(List.of("Brandon Sanderson"));
    }

    @Test
    void shouldReturnPagedRecommendationsExcludingDismissed() {
        // Given
        RecommendationEntity entity = buildRecommendationEntity(UUID.randomUUID(), 85);
        Page<RecommendationEntity> page = new PageImpl<>(List.of(entity));
        when(recommendationRepository.findByStatusOrderByScoreDesc(
                eq(RecommendationStatus.NUEVA), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<RecommendationDto> result = service.getRecommendations(PageRequest.of(0, 20));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().score()).isEqualTo(85);
    }

    @Test
    void shouldDismissRecommendationSuccessfully() {
        // Given
        UUID id = UUID.randomUUID();
        RecommendationEntity entity = buildRecommendationEntity(id, 70);
        when(recommendationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(recommendationRepository.save(entity)).thenReturn(entity);

        // When
        RecommendationDto result = service.dismiss(id);

        // Then
        assertThat(result.status()).isEqualTo(RecommendationStatus.DESCARTADA);
        verify(recommendationRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenDismissingNonExistentRecommendation() {
        // Given
        UUID id = UUID.randomUUID();
        when(recommendationRepository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.dismiss(id))
                .isInstanceOf(LibraryAgentException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void shouldSkipAlreadyScoredBooksInBatch() {
        // Given
        ExtractedBookEntity book = buildEnrichedBook(UUID.randomUUID());
        when(userProfileRepository.findAll()).thenReturn(List.of(userProfile));
        when(readingHistoryRepository.findAllByProfileIdAndStatus(any(), eq(ReadingStatus.READ)))
                .thenReturn(List.of());
        when(extractedBookRepository.findByEnrichedTrueAndConfidenceIn(anyList()))
                .thenReturn(List.of(book));
        when(recommendationRepository.existsByExtractedBookId(book.getId())).thenReturn(true);

        // When
        int processed = service.scoreAndPersistPendingBooks(10);

        // Then
        assertThat(processed).isEqualTo(0);
        assertThat(spyScoringStrategy.callCount).isEqualTo(0);
    }

    @Test
    void shouldProcessUpToMaxBatchBooks() {
        // Given
        ExtractedBookEntity book1 = buildEnrichedBook(UUID.randomUUID());
        ExtractedBookEntity book2 = buildEnrichedBook(UUID.randomUUID());

        when(userProfileRepository.findAll()).thenReturn(List.of(userProfile));
        when(readingHistoryRepository.findAllByProfileIdAndStatus(any(), eq(ReadingStatus.READ)))
                .thenReturn(List.of());
        when(extractedBookRepository.findByEnrichedTrueAndConfidenceIn(anyList()))
                .thenReturn(List.of(book1, book2));
        when(recommendationRepository.existsByExtractedBookId(book1.getId())).thenReturn(false);
        when(recommendationRepository.existsByExtractedBookId(book2.getId())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When — límite de 1 libro
        int processed = service.scoreAndPersistPendingBooks(1);

        // Then
        assertThat(processed).isEqualTo(1);
        assertThat(spyScoringStrategy.callCount).isEqualTo(1);
    }

    // --- helpers ---

    private RecommendationEntity buildRecommendationEntity(UUID id, int score) {
        ExtractedBookEntity book = buildEnrichedBook(UUID.randomUUID());
        RecommendationEntity entity = new RecommendationEntity();
        entity.setExtractedBook(book);
        entity.setScore(score);
        entity.setReasoning("Justificación de prueba");
        entity.setStatus(RecommendationStatus.NUEVA);
        entity.setScoredAt(Instant.now());
        return entity;
    }

    private ExtractedBookEntity buildEnrichedBook(UUID id) {
        ExtractedBookEntity book = new ExtractedBookEntity();
        book.setTitle("El camino de los reyes");
        book.setTitleEs("El camino de los reyes");
        book.setAuthor("Brandon Sanderson");
        book.setAuthorCorrected("Brandon Sanderson");
        book.setEnriched(true);
        book.setConfidence(Confidence.HIGH);
        return book;
    }

    /**
     * Implementación concreta de BookScoringStrategy para tests.
     * Mockito no puede mockear sealed interfaces directamente.
     */
    static final class SpyScoringStrategy implements BookScoringStrategy {
        int callCount = 0;

        @Override
        public ScoringResult score(ExtractedBookEntity book, UserPreferences preferences) {
            callCount++;
            return new ScoringResult(80, "Score de prueba");
        }
    }
}
