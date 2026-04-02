package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.VerifiedTitleDto;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BibliotecaServiceImpl implements BibliotecaService {

    private final VerifiedTitleRepository verifiedTitleRepository;
    private final ExtractedBookRepository extractedBookRepository;

    public BibliotecaServiceImpl(
            VerifiedTitleRepository verifiedTitleRepository,
            ExtractedBookRepository extractedBookRepository) {
        this.verifiedTitleRepository = verifiedTitleRepository;
        this.extractedBookRepository = extractedBookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerifiedTitleDto> findAll() {
        return verifiedTitleRepository.findAllByOrderByNameAsc().stream()
                .map(vt -> {
                    List<String> authors = extractedBookRepository
                            .findByVerifiedTitleAndConfidence(vt, Confidence.VERIFIED)
                            .stream()
                            .flatMap(book -> book.getAuthors().stream())
                            .map(AuthorEntity::getName)
                            .distinct()
                            .sorted()
                            .toList();
                    return VerifiedTitleDto.fromEntity(vt, authors);
                })
                .toList();
    }
}
