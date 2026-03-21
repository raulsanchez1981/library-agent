package com.libraryagent.ingestion.sources;

import java.time.Instant;

record PullpushPost(
        String title,
        String selftext,
        String fullLink,
        String subreddit,
        Instant createdAt
) {}
