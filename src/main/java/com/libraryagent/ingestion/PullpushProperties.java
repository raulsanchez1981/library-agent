package com.libraryagent.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "ingestion.pullpush")
public record PullpushProperties(
        Map<String, String> subreddits,
        int postsSize
) {}
