package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.AuthorRefDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDto;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import com.libraryagent.shared.exception.LibraryAgentException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        // Query 1: todos los títulos ordenados
        List<VerifiedTitleEntity> titles = verifiedTitleRepository.findAllByOrderByNameAsc();

        // Query 2: todos los (vtId, authorName) de libros VERIFIED — evita N+1
        Map<UUID, List<String>> authorsByVtId = extractedBookRepository
                .findVerifiedTitleIdAndAuthorNameByConfidence(Confidence.VERIFIED)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (UUID) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));

        return titles.stream()
                .map(vt -> {
                    List<String> authors = authorsByVtId.getOrDefault(vt.getId(), List.of())
                            .stream()
                            .distinct()
                            .sorted()
                            .toList();
                    return VerifiedTitleDto.fromEntity(vt, authors);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VerifiedTitleDetailDto findById(UUID id) {
        return verifiedTitleRepository.findByIdWithGenres(id)
                .map(vt -> {
                    List<AuthorRefDto> authors = extractedBookRepository
                            .findByVerifiedTitleAndConfidence(vt, Confidence.VERIFIED)
                            .stream()
                            .flatMap(book -> book.getAuthors().stream())
                            .distinct()
                            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                            .map(AuthorRefDto::fromEntity)
                            .toList();
                    return VerifiedTitleDetailDto.fromEntity(vt, authors);
                })
                .orElseThrow(() -> LibraryAgentException.notFound("Título verificado no encontrado: " + id));
    }
}
