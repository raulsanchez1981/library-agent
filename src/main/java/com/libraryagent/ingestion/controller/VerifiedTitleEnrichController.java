package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.EnrichCdlRequest;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
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

    public VerifiedTitleEnrichController(VerifiedTitleEnrichService enrichService) {
        this.enrichService = enrichService;
    }

    @PostMapping("/{id}/enrich-cdl")
    public ResponseEntity<VerifiedTitleDetailDto> enrichFromCdl(
            @PathVariable UUID id,
            @Valid @RequestBody EnrichCdlRequest request) {

        VerifiedTitleDetailDto result = enrichService.enrichFromCdl(id, request.casaDelLibroUrl());
        return ResponseEntity.ok(result);
    }
}
