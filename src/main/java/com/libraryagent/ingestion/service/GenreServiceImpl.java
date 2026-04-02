package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.repository.GenreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    public GenreServiceImpl(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Override
    public List<GenreEntity> findAll() {
        return genreRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    @Transactional
    public GenreEntity findOrCreate(String name) {
        return genreRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> genreRepository.save(new GenreEntity(name)));
    }
}
