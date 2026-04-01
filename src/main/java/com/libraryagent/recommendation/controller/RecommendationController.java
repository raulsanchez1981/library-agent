package com.libraryagent.recommendation.controller;

import com.libraryagent.recommendation.model.RecommendationDto;
import com.libraryagent.recommendation.service.RecommendationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
    public ResponseEntity<Page<RecommendationDto>> getRecommendations(
            @PageableDefault(size = 20, sort = "score", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(recommendationService.getRecommendations(pageable));
    }

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecommendationDto> dismiss(@PathVariable UUID id) {
        return ResponseEntity.ok(recommendationService.dismiss(id));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> trigger(
            @RequestParam(defaultValue = "20") int maxBatch) {
        int processed = recommendationService.scoreAndPersistPendingBooks(maxBatch);
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
