package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDto;

import java.util.List;
import java.util.UUID;

public interface BibliotecaService {

    List<VerifiedTitleDto> findAll();

    VerifiedTitleDetailDto findById(UUID id);
}
