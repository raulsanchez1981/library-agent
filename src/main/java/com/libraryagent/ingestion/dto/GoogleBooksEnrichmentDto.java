package com.libraryagent.ingestion.dto;

public record GoogleBooksEnrichmentDto(
        String googleBooksId,
        String coverUrl,
        String synopsis,
        String publisher,
        String publishedDate,
        Integer pageCount,
        String isbn
) {}
