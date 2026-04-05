package com.libraryagent.ingestion.model;

import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "extracted_books")
@Getter
@Setter
@NoArgsConstructor
public class ExtractedBookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(length = 256)
    private String author;

    @Column(name = "is_saga", nullable = false)
    private boolean isSaga;

    // --- Campos de enriquecimiento (rellenados por BookEnrichmentService) ---

    @Column(name = "title_es", length = 512)
    private String titleEs;

    @Column(name = "title_es_ol", length = 500)
    private String titleEsOl;

    @Column(name = "author_corrected", length = 300)
    private String authorCorrected;

    @Column(name = "available_in_spanish", nullable = false)
    private boolean availableInSpanish;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrichment_source", length = 20)
    private EnrichmentSource enrichmentSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", length = 10)
    private Confidence confidence;

    @Column(name = "enriched", nullable = false)
    private boolean enriched;

    @Column(name = "enriched_at")
    private Instant enrichedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_title_id")
    private VerifiedTitleEntity verifiedTitle;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private List<AuthorEntity> authors = new ArrayList<>();

    // --- Metadatos ---

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_mention_id", nullable = false)
    private RawMentionEntity sourceMention;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
