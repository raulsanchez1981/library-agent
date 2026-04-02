package com.libraryagent.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
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

    public VerifiedTitleEntity(String name) {
        this.name = name;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }
}
