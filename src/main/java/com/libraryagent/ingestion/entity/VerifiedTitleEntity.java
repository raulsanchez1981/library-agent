package com.libraryagent.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "verified_titles")
@Getter
@Setter
@NoArgsConstructor
public class VerifiedTitleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "google_books_id", length = 64)
    private String googleBooksId;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "casa_del_libro_url", length = 1024)
    private String casaDelLibroUrl;

    @Column(name = "technical_sheet", columnDefinition = "TEXT")
    private String technicalSheet;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "book_genres",
        joinColumns = @JoinColumn(name = "verified_title_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<GenreEntity> genres = new ArrayList<>();

    public VerifiedTitleEntity(String name) {
        this.name = name;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }
}
