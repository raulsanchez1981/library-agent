package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.VerifiedTitleDto;

import java.util.List;

public interface BibliotecaService {

    List<VerifiedTitleDto> findAll();
}
