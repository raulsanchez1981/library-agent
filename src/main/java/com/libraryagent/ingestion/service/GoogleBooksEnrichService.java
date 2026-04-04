package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.GoogleBooksEnrichmentDto;

import java.util.Optional;

public interface GoogleBooksEnrichService {

    Optional<GoogleBooksEnrichmentDto> findEnrichmentData(String titleEs, String author);
}
