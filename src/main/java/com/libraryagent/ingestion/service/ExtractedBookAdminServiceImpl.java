package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.ExtractedBookAdminDto;
import com.libraryagent.ingestion.dto.UpdateExtractedBookRequest;
import com.libraryagent.ingestion.entity.AuthorEntity;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.extractor.Confidence;
import com.libraryagent.ingestion.extractor.EnrichmentSource;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.AuthorRepository;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import com.libraryagent.shared.exception.LibraryAgentException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class ExtractedBookAdminServiceImpl implements ExtractedBookAdminService {

    private final ExtractedBookRepository repository;
    private final AuthorRepository authorRepository;
    private final VerifiedTitleRepository verifiedTitleRepository;
    private final GenreEnrichmentService genreEnrichmentService;
    private final BookLinkingService bookLinkingService;

    public ExtractedBookAdminServiceImpl(
            ExtractedBookRepository repository,
            AuthorRepository authorRepository,
            VerifiedTitleRepository verifiedTitleRepository,
            GenreEnrichmentService genreEnrichmentService,
            BookLinkingService bookLinkingService) {
        this.repository = repository;
        this.authorRepository = authorRepository;
        this.verifiedTitleRepository = verifiedTitleRepository;
        this.genreEnrichmentService = genreEnrichmentService;
        this.bookLinkingService = bookLinkingService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExtractedBookAdminDto> findAll(String search, Confidence confidence, Boolean enriched, Pageable pageable) {
        Specification<ExtractedBookEntity> spec = searchSpec(search)
                .and(confidenceSpec(confidence))
                .and(enrichedSpec(enriched))
                .and(notDiscardedSpec());

        return repository.findAll(spec, pageable).map(ExtractedBookAdminDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ExtractedBookAdminDto findById(UUID id) {
        return repository.findById(id)
                .map(ExtractedBookAdminDto::fromEntity)
                .orElseThrow(() -> LibraryAgentException.notFound("ExtractedBook no encontrado: " + id));
    }

    @Override
    @Transactional
    public ExtractedBookAdminDto update(UUID id, UpdateExtractedBookRequest request) {
        ExtractedBookEntity entity = repository.findById(id)
                .orElseThrow(() -> LibraryAgentException.notFound("ExtractedBook no encontrado: " + id));

        if (request.titleEs() != null) {
            entity.setTitleEs(request.titleEs());
        }
        if (request.authorCorrected() != null) {
            entity.setAuthorCorrected(request.authorCorrected());
            if (!request.authorCorrected().isBlank()) {
                List<AuthorEntity> newAuthors = AuthorNameParser.parse(request.authorCorrected()).stream()
                        .map(name -> {
                            String normalized = TitleCaseNormalizer.normalize(name);
                            return authorRepository.findByNameIgnoreCase(normalized)
                                    .orElseGet(() -> authorRepository.save(new AuthorEntity(normalized)));
                        })
                        .toList();
                entity.getAuthors().clear();
                entity.getAuthors().addAll(newAuthors);
            }
        }
        if (request.availableInSpanish() != null) {
            entity.setAvailableInSpanish(request.availableInSpanish());
        }
        if (request.isSaga() != null) {
            entity.setSaga(request.isSaga());
        }

        // El admin siempre verifica: confidence → VERIFIED, fuente → ADMIN
        entity.setConfidence(Confidence.VERIFIED);
        entity.setEnrichmentSource(EnrichmentSource.ADMIN);

        // Find-or-create VerifiedTitle si titleEs tiene valor
        if (StringUtils.hasText(request.titleEs())) {
            String normalized = TitleCaseNormalizer.normalize(request.titleEs());
            VerifiedTitleEntity vt = verifiedTitleRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> verifiedTitleRepository.save(new VerifiedTitleEntity(normalized)));
            entity.setVerifiedTitle(vt);

        }

        // Marcar autores vinculados como verified
        entity.getAuthors().forEach(a -> {
            if (!a.isVerified()) {
                a.setVerified(true);
                authorRepository.save(a);
            }
        });

        repository.save(entity);

        // Vincular libros no verificados que coincidan con los títulos/autores ya verificados
        linkUnverifiedBooks();

        // Enriquecer géneros del VerifiedTitle de forma asíncrona
        if (entity.getVerifiedTitle() != null) {
            genreEnrichmentService.enrichSingle(entity.getVerifiedTitle());
        }

        return ExtractedBookAdminDto.fromEntity(entity);
    }

    @Override
    @Transactional
    public void discard(UUID id) {
        ExtractedBookEntity entity = repository.findById(id)
                .orElseThrow(() -> LibraryAgentException.notFound("ExtractedBook no encontrado: " + id));
        entity.setDiscarded(true);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void linkUnverifiedBooks() {
        bookLinkingService.linkUnverifiedBooks();
    }

    // --- Specifications ---

    private Specification<ExtractedBookEntity> searchSpec(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("titleEs")), pattern),
                cb.like(cb.lower(root.get("authorCorrected")), pattern)
        );
    }

    private Specification<ExtractedBookEntity> confidenceSpec(Confidence confidence) {
        if (confidence == null) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> cb.equal(root.get("confidence"), confidence);
    }

    private Specification<ExtractedBookEntity> enrichedSpec(Boolean enriched) {
        if (enriched == null) {
            return (root, query, cb) -> null;
        }
        return (root, query, cb) -> cb.equal(root.get("enriched"), enriched);
    }

    private Specification<ExtractedBookEntity> notDiscardedSpec() {
        return (root, query, cb) -> cb.isFalse(root.get("discarded"));
    }
}
