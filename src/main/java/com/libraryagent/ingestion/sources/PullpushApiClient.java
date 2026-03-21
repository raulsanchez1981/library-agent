package com.libraryagent.ingestion.sources;

import java.util.List;

interface PullpushApiClient {

    List<PullpushPost> fetchPosts(String subreddit);
}
