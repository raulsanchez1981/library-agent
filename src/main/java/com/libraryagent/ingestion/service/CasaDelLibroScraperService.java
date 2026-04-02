package com.libraryagent.ingestion.service;

import com.libraryagent.ingestion.dto.CdlEnrichmentResultDto;

public interface CasaDelLibroScraperService {

    CdlEnrichmentResultDto scrape(String url);
}
