package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.AuthorDetailDto;
import com.libraryagent.ingestion.dto.AuthorSummaryDto;

import java.util.List;
import java.util.UUID;

public interface AuthorService {

    List<AuthorSummaryDto> findAll();

    AuthorDetailDto findById(UUID id);
}
