package com.libraryagent.ingestion.sources;

import com.libraryagent.ingestion.model.RawMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedditIngester implements BookSourceIngester {

    private static final Logger log = LoggerFactory.getLogger(RedditIngester.class);

    @Value("${ingestion.reddit.client-id:#{null}}")
    private String clientId;

    @Value("${ingestion.reddit.client-secret:#{null}}")
    private String clientSecret;

    @Override
    public String sourceId() {
        return "reddit";
    }

    @Override
    public List<RawMention> ingest() {
        // TODO: implementar llamada a Reddit API v2
        // Subreddits de interés: r/booksuggestions, r/books, r/scifi
        log.warn("RedditIngester no implementado aún");
        return List.of();
    }

    @Override
    public boolean isAvailable() {
        // TODO: verificar token de acceso OAuth2 con Reddit
        return clientId != null && clientSecret != null;
    }
}
