package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import com.libraryagent.ingestion.dto.EnrichCdlRequest;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.service.CasaDelLibroScraperService;
import com.libraryagent.ingestion.service.VerifiedTitleEnrichService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/verified-titles")
@PreAuthorize("hasRole('ADMIN')")
public class VerifiedTitleEnrichController {

    private final VerifiedTitleEnrichService enrichService;
    private final CasaDelLibroScraperService scraperService;

    public VerifiedTitleEnrichController(
            VerifiedTitleEnrichService enrichService,
            CasaDelLibroScraperService scraperService) {
        this.enrichService = enrichService;
        this.scraperService = scraperService;
    }

    @PostMapping("/{id}/enrich-cdl")
    public ResponseEntity<VerifiedTitleDetailDto> enrichFromCdl(
            @PathVariable UUID id,
            @Valid @RequestBody EnrichCdlRequest request) {

        VerifiedTitleDetailDto result = enrichService.enrichFromCdl(id, request.casaDelLibroUrl());
        return ResponseEntity.ok(result);
    }

    /** Preview del scraper sin persistir — útil para diagnosticar qué extrae CDL */
    @PostMapping("/scrape-preview")
    public ResponseEntity<CdlEnrichmentResultDto> scrapePreview(
            @Valid @RequestBody EnrichCdlRequest request) {

        return ResponseEntity.ok(scraperService.scrape(request.casaDelLibroUrl()));
    }
}
