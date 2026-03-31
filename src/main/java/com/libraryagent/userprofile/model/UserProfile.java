package com.libraryagent.userprofile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage = "es";

    @Column(name = "min_score_threshold", nullable = false, precision = 3, scale = 2)
    private BigDecimal minScoreThreshold = new BigDecimal("0.75");

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_favorite_genres", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "genre", length = 100)
    private List<String> favoriteGenres = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_favorite_authors", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "author", length = 256)
    private List<String> favoriteAuthors = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
