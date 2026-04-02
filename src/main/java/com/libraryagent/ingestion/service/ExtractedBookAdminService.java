package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.ExtractedBookAdminDto;
import com.libraryagent.ingestion.dto.UpdateExtractedBookRequest;
import com.libraryagent.ingestion.extractor.Confidence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ExtractedBookAdminService {

    Page<ExtractedBookAdminDto> findAll(String search, Confidence confidence, Boolean enriched, Pageable pageable);

    ExtractedBookAdminDto findById(UUID id);

    ExtractedBookAdminDto update(UUID id, UpdateExtractedBookRequest request);

    void linkUnverifiedBooks();

    void enrichVerifiedTitlesWithoutCover();
}
