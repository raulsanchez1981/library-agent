package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.AuthorDetailDto;
import com.libraryagent.ingestion.dto.AuthorSummaryDto;
import com.libraryagent.ingestion.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public ResponseEntity<List<AuthorSummaryDto>> findAll() {
        return ResponseEntity.ok(authorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorDetailDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(authorService.findById(id));
    }
}
