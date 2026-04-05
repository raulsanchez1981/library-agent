package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.GenreEntity;

import java.util.List;

public interface GenreService {

    List<GenreEntity> findAll();

    GenreEntity findOrCreate(String name);
}
