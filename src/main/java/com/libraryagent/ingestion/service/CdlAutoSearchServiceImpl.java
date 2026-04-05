package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.client.GoogleBooksClient;
import com.libraryagent.ingestion.dto.GoogleBooksEnrichmentDto;
import com.libraryagent.ingestion.entity.VerifiedTitleEntity;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.ingestion.repository.ExtractedBookRepository;
import com.libraryagent.ingestion.repository.VerifiedTitleRepository;
import com.libraryagent.shared.exception.LibraryAgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CdlAutoSearchServiceImpl implements CdlAutoSearchService {

    private static final Logger log = LoggerFactory.getLogger(CdlAutoSearchServiceImpl.class);
    private static final long DELAY_BETWEEN_SEARCHES_MS = 2_000;

    private final GoogleBooksClient googleBooksClient;
    private final VerifiedTitleEnrichService enrichService;
    private final VerifiedTitleRepository verifiedTitleRepository;
    private final ExtractedBookRepository extractedBookRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CdlAutoSearchServiceImpl(
            GoogleBooksClient googleBooksClient,
            VerifiedTitleEnrichService enrichService,
            VerifiedTitleRepository verifiedTitleRepository,
            ExtractedBookRepository extractedBookRepository,
            ApplicationEventPublisher eventPublisher) {
        this.googleBooksClient = googleBooksClient;
        this.enrichService = enrichService;
        this.verifiedTitleRepository = verifiedTitleRepository;
        this.extractedBookRepository = extractedBookRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Async("cdlSearchExecutor")
    public void searchAndEnrichAll() {
        List<UUID> pending = verifiedTitleRepository.findAllPendingCdlSearch();
        log.info("Enriquecimiento automático: {} títulos pendientes", pending.size());
        if (pending.isEmpty()) return;
        doEnrichBatch(pending);
    }

    @Override
    @Async("cdlSearchExecutor")
    public void searchAndEnrich(List<UUID> verifiedTitleIds) {
        log.info("Enriquecimiento automático para {} títulos", verifiedTitleIds.size());
        doEnrichBatch(verifiedTitleIds);
    }

    @Override
    @Async("cdlSearchExecutor")
    public void reEnrichAllGoogleBooks() {
        List<UUID> ids = verifiedTitleRepository.findAllAutoEnrichedWithoutCdl();
        log.info("Re-enriquecimiento forzado: {} títulos con estado AUTO", ids.size());
        if (ids.isEmpty()) return;
        doEnrichBatch(ids);
    }

    private void doEnrichBatch(List<UUID> ids) {
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                try {
                    Thread.sleep(DELAY_BETWEEN_SEARCHES_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch interrumpido en el título {}/{}", i + 1, ids.size());
                    return;
                }
            }

            UUID vtId = ids.get(i);
            try {
                CdlAutoSearchStatus status = processOne(vtId);
                eventPublisher.publishEvent(new CdlSearchProgressEvent(vtId, status));
                log.info("[{}/{}] vtId={} → {}", i + 1, ids.size(), vtId, status);
            } catch (Exception e) {
                log.error("Error inesperado procesando verifiedTitleId={}: {}", vtId, e.getMessage());
            }
        }
        log.info("Enriquecimiento automático completado para {} títulos", ids.size());
    }

    private CdlAutoSearchStatus processOne(UUID vtId) {
        List<ExtractedBookEntity> candidates = extractedBookRepository
                .findByVerifiedTitleIdWithSearchData(vtId, PageRequest.of(0, 1));

        if (candidates.isEmpty()) {
            log.warn("verifiedTitleId={} no tiene ExtractedBook con titleEs y autor", vtId);
            markNotFound(vtId);
            return CdlAutoSearchStatus.NOT_FOUND;
        }

        ExtractedBookEntity book = candidates.getFirst();
        String titleEs = book.getTitleEs();
        String author = book.getAuthorCorrected() != null ? book.getAuthorCorrected() : book.getAuthor();

        Optional<GoogleBooksEnrichmentDto> enrichmentData =
                googleBooksClient.findBestSpanishVolume(titleEs, author);

        if (enrichmentData.isPresent()) {
            enrichService.enrichFromGoogleBooks(vtId, enrichmentData.get());
            return CdlAutoSearchStatus.AUTO;
        } else {
            log.info("Google Books no encontró volumen para vtId={} (titleEs='{}')", vtId, titleEs);
            markNotFound(vtId);
            return CdlAutoSearchStatus.NOT_FOUND;
        }
    }

    @Transactional
    protected void markNotFound(UUID vtId) {
        VerifiedTitleEntity vt = verifiedTitleRepository.findById(vtId)
                .orElseThrow(() -> LibraryAgentException.notFound("Título verificado no encontrado: " + vtId));
        vt.setCdlAutoSearchStatus(CdlAutoSearchStatus.NOT_FOUND);
        verifiedTitleRepository.save(vt);
    }
}
