package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.client.GoogleBooksClient;
import com.libraryagent.ingestion.dto.GoogleBooksEnrichmentDto;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleBooksEnrichServiceImpl implements GoogleBooksEnrichService {

    private final GoogleBooksClient googleBooksClient;

    public GoogleBooksEnrichServiceImpl(GoogleBooksClient googleBooksClient) {
        this.googleBooksClient = googleBooksClient;
    }

    @Override
    public Optional<GoogleBooksEnrichmentDto> findEnrichmentData(String titleEs, String author) {
        return googleBooksClient.findBestSpanishVolume(titleEs, author);
    }
}
