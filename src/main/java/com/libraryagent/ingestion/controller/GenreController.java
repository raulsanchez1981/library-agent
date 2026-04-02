package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.CreateGenreRequest;
import com.libraryagent.ingestion.dto.GenreDto;
import com.libraryagent.ingestion.service.GenreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GenreDto>> findAll() {
        List<GenreDto> genres = genreService.findAll().stream()
                .map(GenreDto::fromEntity)
                .toList();
        return ResponseEntity.ok(genres);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenreDto> create(@Valid @RequestBody CreateGenreRequest request) {
        GenreDto created = GenreDto.fromEntity(genreService.findOrCreate(request.name()));
        URI location = URI.create("/api/v1/genres/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
