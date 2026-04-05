package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.AuthorBookDto;
import com.libraryagent.ingestion.dto.AuthorDetailDto;
import com.libraryagent.ingestion.dto.AuthorSummaryDto;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final VerifiedTitleRepository verifiedTitleRepository;

    public AuthorServiceImpl(AuthorRepository authorRepository, VerifiedTitleRepository verifiedTitleRepository) {
        this.authorRepository = authorRepository;
        this.verifiedTitleRepository = verifiedTitleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorSummaryDto> findAll() {
        return authorRepository.findAllWithVerifiedBooks().stream()
                .map(author -> {
                    int bookCount = verifiedTitleRepository.findAllByAuthorId(author.getId()).size();
                    return AuthorSummaryDto.fromEntity(author, bookCount);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorDetailDto findById(UUID id) {
        return authorRepository.findById(id)
                .map(author -> {
                    List<AuthorBookDto> books = verifiedTitleRepository.findAllByAuthorId(id).stream()
                            .map(AuthorBookDto::fromEntity)
                            .toList();
                    return AuthorDetailDto.fromEntity(author, books);
                })
                .orElseThrow(() -> new EntityNotFoundException("Author not found: " + id));
    }
}
