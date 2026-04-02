package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDto;
import com.libraryagent.ingestion.service.BibliotecaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biblioteca")
public class BibliotecaController {

    private final BibliotecaService bibliotecaService;

    public BibliotecaController(BibliotecaService bibliotecaService) {
        this.bibliotecaService = bibliotecaService;
    }

    @GetMapping
    public ResponseEntity<List<VerifiedTitleDto>> findAll() {
        return ResponseEntity.ok(bibliotecaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VerifiedTitleDetailDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(bibliotecaService.findById(id));
    }
}
