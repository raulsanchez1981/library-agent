package com.libraryagent.ingestion.scheduler;

import com.libraryagent.ingestion.service.BookEnrichmentService;
import com.libraryagent.ingestion.service.GenreEnrichmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookEnrichmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookEnrichmentScheduler.class);

    private final BookEnrichmentService enrichmentService;
    private final GenreEnrichmentService genreEnrichmentService;

    public BookEnrichmentScheduler(
            BookEnrichmentService enrichmentService,
            GenreEnrichmentService genreEnrichmentService) {
        this.enrichmentService = enrichmentService;
        this.genreEnrichmentService = genreEnrichmentService;
    }

    @Scheduled(cron = "0 30 8 * * *")
    public void scheduledEnrichment() {
        log.info("Iniciando enriquecimiento programado");
        enrichmentService.enrichPending();
        int updated = genreEnrichmentService.enrichMissingGenres();
        log.info("Géneros enriquecidos en scheduler: {} títulos actualizados", updated);
    }
}
