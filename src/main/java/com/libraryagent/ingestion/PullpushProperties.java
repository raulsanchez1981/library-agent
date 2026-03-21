package com.libraryagent.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ingestion.pullpush")
public record PullpushProperties(
        List<String> subreddits,
        int postsSize
) {}
