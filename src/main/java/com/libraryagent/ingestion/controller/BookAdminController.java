package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.ExtractedBookAdminDto;
import com.libraryagent.ingestion.dto.UpdateExtractedBookRequest;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.service.ExtractedBookAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/books")
@PreAuthorize("hasRole('ADMIN')")
public class BookAdminController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title", "titleEs", "authorCorrected", "confidence",
            "enrichmentSource", "enriched", "createdAt"
    );

    private final ExtractedBookAdminService adminService;

    public BookAdminController(ExtractedBookAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<Page<ExtractedBookAdminDto>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Confidence confidence,
            @RequestParam(required = false) Boolean enriched,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String safeField = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, safeField));
        return ResponseEntity.ok(adminService.findAll(search, confidence, enriched, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtractedBookAdminDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExtractedBookAdminDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExtractedBookRequest request) {

        return ResponseEntity.ok(adminService.update(id, request));
    }
}
