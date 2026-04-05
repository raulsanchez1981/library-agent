package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.AutorDetailDto;
import com.libraryagent.ingestion.dto.AutorDto;

import java.util.List;
import java.util.UUID;

public interface AutorService {

    List<AutorDto> findAll();

    AutorDetailDto findById(UUID id);
}
