package com.libraryagent.ingestion.dto;

import java.util.List;

public record GoogleBooksEnrichmentDto(
        String googleBooksId,
        String coverUrl,
        String synopsis,
        String publisher,
        String publishedDate,
        Integer pageCount,
        String isbn,
        List<String> categories
) {}
