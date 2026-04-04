package com.libraryagent.ingestion.service;

import java.util.UUID;

public record CdlSearchProgressEvent(UUID verifiedTitleId, CdlAutoSearchStatus status) {}
