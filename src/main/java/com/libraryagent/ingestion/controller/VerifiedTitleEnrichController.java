package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.CdlAutoSearchRequest;
import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import com.libraryagent.ingestion.dto.EnrichCdlRequest;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.service.CasaDelLibroScraperService;
import com.libraryagent.ingestion.service.CdlAutoSearchService;
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
    private final CdlAutoSearchService cdlAutoSearchService;

    public VerifiedTitleEnrichController(
            VerifiedTitleEnrichService enrichService,
            CasaDelLibroScraperService scraperService,
            CdlAutoSearchService cdlAutoSearchService) {
        this.enrichService = enrichService;
        this.scraperService = scraperService;
        this.cdlAutoSearchService = cdlAutoSearchService;
    }

    /** Enriquecimiento manual desde URL de Casa del Libro. Marca como CONFIRMED. */
    @PostMapping("/{id}/enrich-cdl")
    public ResponseEntity<VerifiedTitleDetailDto> enrichFromCdl(
            @PathVariable UUID id,
            @Valid @RequestBody EnrichCdlRequest request) {
        return ResponseEntity.ok(enrichService.enrichFromCdl(id, request.casaDelLibroUrl()));
    }

    /** Confirma el libro tal como está (sin CDL). Marca como CONFIRMED. */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<VerifiedTitleDetailDto> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(enrichService.confirm(id));
    }

    /** Dispara el enriquecimiento automático (Google Books) para una lista de títulos. 202 inmediato. */
    @PostMapping("/cdl-auto-search")
    public ResponseEntity<Void> cdlAutoSearch(@Valid @RequestBody CdlAutoSearchRequest request) {
        cdlAutoSearchService.searchAndEnrich(request.verifiedTitleIds());
        return ResponseEntity.accepted().build();
    }

    /** Igual que cdlAutoSearch pero sobre TODOS los títulos pendientes. */
    @PostMapping("/cdl-auto-search-all")
    public ResponseEntity<Void> cdlAutoSearchAll() {
        cdlAutoSearchService.searchAndEnrichAll();
        return ResponseEntity.accepted().build();
    }

    /** Re-enriquece todos los títulos AUTO (sin CDL) desde Google Books, aunque ya tengan datos. */
    @PostMapping("/re-enrich-google-books")
    public ResponseEntity<Void> reEnrichGoogleBooks() {
        cdlAutoSearchService.reEnrichAllGoogleBooks();
        return ResponseEntity.accepted().build();
    }

    /** Preview del scraper CDL sin persistir — para diagnóstico. */
    @PostMapping("/scrape-preview")
    public ResponseEntity<CdlEnrichmentResultDto> scrapePreview(
            @Valid @RequestBody EnrichCdlRequest request) {
        return ResponseEntity.ok(scraperService.scrape(request.casaDelLibroUrl()));
    }
}
