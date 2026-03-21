package com.libraryagent.ingestion.sources;

import java.time.Instant;

record PullpushPost(
        String title,
        String selftext,
        String url,
        String subreddit,
        Instant createdAt
) {}
