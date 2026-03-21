package com.libraryagent.recommendation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "book_scores")
@Getter
@Setter
@NoArgsConstructor
public class BookScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "book_title", nullable = false, length = 512)
    private String bookTitle;

    @Column(name = "book_author", length = 256)
    private String bookAuthor;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;
}
