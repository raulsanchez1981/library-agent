package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;
import com.libraryagent.ingestion.dto.GoogleBooksEnrichmentDto;
import com.libraryagent.ingestion.dto.VerifiedTitleDetailDto;
import com.libraryagent.ingestion.entity.GenreEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VerifiedTitleEnrichServiceImpl implements VerifiedTitleEnrichService {

    private final VerifiedTitleRepository verifiedTitleRepository;
    private final CasaDelLibroScraperService scraperService;
    private final GenreService genreService;

    public VerifiedTitleEnrichServiceImpl(
            VerifiedTitleRepository verifiedTitleRepository,
            CasaDelLibroScraperService scraperService,
            GenreService genreService) {
        this.verifiedTitleRepository = verifiedTitleRepository;
        this.scraperService = scraperService;
        this.genreService = genreService;
    }

    @Override
    @Transactional
    public VerifiedTitleDetailDto enrichFromCdl(UUID id, String casaDelLibroUrl) {
        VerifiedTitleEntity vt = verifiedTitleRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new EntityNotFoundException("Título verificado no encontrado: " + id));

        CdlEnrichmentResultDto result = scraperService.scrape(casaDelLibroUrl);

        vt.setCoverUrl(result.coverUrl());
        vt.setSynopsis(result.synopsis());
        vt.setTechnicalSheet(result.technicalSheet());
        vt.setCasaDelLibroUrl(casaDelLibroUrl);
        vt.setCdlAutoSearchStatus(CdlAutoSearchStatus.CONFIRMED);

        if (result.genres() != null) {
            List<String> existingNames = vt.getGenres().stream()
                    .map(GenreEntity::getName)
                    .map(String::toLowerCase)
                    .toList();
            result.genres().stream()
                    .filter(name -> !existingNames.contains(name.toLowerCase()))
                    .map(genreService::findOrCreate)
                    .forEach(vt.getGenres()::add);
        }

        return VerifiedTitleDetailDto.fromEntity(verifiedTitleRepository.save(vt));
    }

    @Override
    @Transactional
    public VerifiedTitleDetailDto enrichFromGoogleBooks(UUID id, GoogleBooksEnrichmentDto data) {
        VerifiedTitleEntity vt = verifiedTitleRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new EntityNotFoundException("Título verificado no encontrado: " + id));

        vt.setGoogleBooksId(data.googleBooksId());
        vt.setCoverUrl(data.coverUrl());
        vt.setSynopsis(data.synopsis());
        vt.setPublisher(data.publisher());
        vt.setPublishedDate(data.publishedDate());
        vt.setPageCount(data.pageCount());
        vt.setIsbn(data.isbn());
        vt.setCdlAutoSearchStatus(CdlAutoSearchStatus.AUTO);

        if (data.categories() != null && !data.categories().isEmpty()) {
            List<String> existingNames = vt.getGenres().stream()
                    .map(GenreEntity::getName)
                    .map(String::toLowerCase)
                    .toList();
            data.categories().stream()
                    .filter(name -> !existingNames.contains(name.toLowerCase()))
                    .map(genreService::findOrCreate)
                    .forEach(vt.getGenres()::add);
        }

        return VerifiedTitleDetailDto.fromEntity(verifiedTitleRepository.save(vt));
    }

    @Override
    @Transactional
    public VerifiedTitleDetailDto confirm(UUID id) {
        VerifiedTitleEntity vt = verifiedTitleRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new EntityNotFoundException("Título verificado no encontrado: " + id));

        vt.setCdlAutoSearchStatus(CdlAutoSearchStatus.CONFIRMED);

        return VerifiedTitleDetailDto.fromEntity(verifiedTitleRepository.save(vt));
    }
}
