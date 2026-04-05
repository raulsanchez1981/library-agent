package com.libraryagent.ingestion.service;

public interface BookEnrichmentService {

    long countPendingEnrichment();

    void enrichPending();

    int reEnrichAuthors();
}
