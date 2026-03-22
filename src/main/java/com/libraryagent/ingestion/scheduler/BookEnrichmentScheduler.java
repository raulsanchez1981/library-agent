package com.libraryagent.ingestion.scheduler;

import com.libraryagent.ingestion.service.BookEnrichmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookEnrichmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookEnrichmentScheduler.class);

    private final BookEnrichmentService enrichmentService;

    public BookEnrichmentScheduler(BookEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @Scheduled(cron = "0 30 8 * * *")
    public void scheduledEnrichment() {
        log.info("Iniciando enriquecimiento programado");
        enrichmentService.enrichPending();
    }
}
