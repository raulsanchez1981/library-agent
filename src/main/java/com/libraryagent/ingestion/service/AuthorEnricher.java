package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
import org.springframework.stereotype.Component;

@Component
public class AuthorEnricher {

    private final AuthorRepository authorRepository;

    public AuthorEnricher(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public AuthorEntity findOrCreate(String name) {
        String normalized = TitleCaseNormalizer.normalize(name);
        return authorRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> authorRepository.save(new AuthorEntity(normalized)));
    }
}
