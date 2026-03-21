package com.libraryagent.ingestion.sources;

import com.libraryagent.ingestion.PullpushProperties;
import com.libraryagent.ingestion.model.RawMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PullpushIngester implements BookSourceIngester {

    private static final Logger log = LoggerFactory.getLogger(PullpushIngester.class);

    private final PullpushApiClient pullpushApiClient;
    private final PullpushProperties properties;

    PullpushIngester(PullpushApiClient pullpushApiClient, PullpushProperties properties) {
        this.pullpushApiClient = pullpushApiClient;
        this.properties = properties;
    }

    @Override
    public String sourceId() {
        return "reddit";
    }

    @Override
    public List<RawMention> ingest() {
        return properties.subreddits().stream()
                .flatMap(subreddit -> fetchMentions(subreddit).stream())
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private List<RawMention> fetchMentions(String subreddit) {
        try {
            log.info("Ingiriendo r/{} via Pullpush", subreddit);
            return pullpushApiClient.fetchPosts(subreddit).stream()
                    .map(post -> new RawMention(
                            UUID.randomUUID(),
                            "reddit/" + subreddit,
                            post.title() + " " + post.selftext(),
                            post.url(),
                            post.createdAt()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("Error al ingestar r/{}: {}", subreddit, e.getMessage());
            return List.of();
        }
    }
}
