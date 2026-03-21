package com.libraryagent.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "raw_mentions")
@Getter
@Setter
@NoArgsConstructor
public class RawMentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(length = 512)
    private String url;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
