package com.libraryagent.ingestion.controller;

import com.libraryagent.ingestion.dto.BookSearchResultDto;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
public class BookSearchController {

    private final ExtractedBookRepository bookRepository;

    public BookSearchController(ExtractedBookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
    public ResponseEntity<List<BookSearchResultDto>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        if (q == null || q.strip().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<BookSearchResultDto> results = bookRepository
                .searchByTitle(q.strip(), PageRequest.of(0, Math.min(limit, 20)))
                .stream()
                .map(BookSearchResultDto::fromEntity)
                .toList();

        return ResponseEntity.ok(results);
    }
}
