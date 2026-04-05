package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.AutorBookDto;
import com.libraryagent.ingestion.dto.AutorDetailDto;
import com.libraryagent.ingestion.dto.AutorDto;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AutorServiceImpl implements AutorService {

    private final AuthorRepository authorRepository;
    private final VerifiedTitleRepository verifiedTitleRepository;

    public AutorServiceImpl(AuthorRepository authorRepository, VerifiedTitleRepository verifiedTitleRepository) {
        this.authorRepository = authorRepository;
        this.verifiedTitleRepository = verifiedTitleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutorDto> findAll() {
        return authorRepository.findAllWithVerifiedBooks().stream()
                .map(author -> {
                    int bookCount = verifiedTitleRepository.findAllByAuthorId(author.getId()).size();
                    return AutorDto.fromEntity(author, bookCount);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AutorDetailDto findById(UUID id) {
        return authorRepository.findById(id)
                .map(author -> {
                    List<AutorBookDto> books = verifiedTitleRepository.findAllByAuthorId(id).stream()
                            .map(AutorBookDto::fromEntity)
                            .toList();
                    return AutorDetailDto.fromEntity(author, books);
                })
                .orElseThrow(() -> new EntityNotFoundException("Autor no encontrado: " + id));
    }
}
