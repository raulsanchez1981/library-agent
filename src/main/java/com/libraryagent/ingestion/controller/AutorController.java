package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.AutorDetailDto;
import com.libraryagent.ingestion.dto.AutorDto;
import com.libraryagent.ingestion.service.AutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/autores")
public class AutorController {

    private final AutorService autorService;

    public AutorController(AutorService autorService) {
        this.autorService = autorService;
    }

    @GetMapping
    public ResponseEntity<List<AutorDto>> findAll() {
        return ResponseEntity.ok(autorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutorDetailDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(autorService.findById(id));
    }
}
