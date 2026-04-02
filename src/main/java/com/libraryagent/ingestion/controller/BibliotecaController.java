package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.VerifiedTitleDto;
import com.libraryagent.ingestion.service.BibliotecaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
