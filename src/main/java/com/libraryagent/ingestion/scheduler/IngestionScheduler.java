package com.libraryagent.ingestion.scheduler;

import com.libraryagent.ingestion.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final IngestionService ingestionService;

    public IngestionScheduler(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scheduledIngestion() {
        log.info("Iniciando ingesta programada");
        ingestionService.runFullIngestion();
    }
}
