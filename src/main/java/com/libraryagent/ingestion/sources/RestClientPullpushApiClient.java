package com.libraryagent.ingestion.sources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.libraryagent.ingestion.PullpushProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

@Component
class RestClientPullpushApiClient implements PullpushApiClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientPullpushApiClient.class);
    private static final String BASE_URL = "https://api.pullpush.io/reddit/search/submission/";

    private final PullpushProperties properties;
    private final RestClient restClient;

    RestClientPullpushApiClient(PullpushProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public List<PullpushPost> fetchPosts(String subreddit) {
        try {
            PullpushResponse response = restClient.get()
                    .uri(BASE_URL + "?subreddit={sub}&size={size}&sort=desc", subreddit, properties.postsSize())
                    .retrieve()
                    .body(PullpushResponse.class);

            if (response == null || response.data() == null) {
                return List.of();
            }

            return response.data().stream()
                    .map(d -> new PullpushPost(
                            d.title() != null ? d.title() : "",
                            d.selftext() != null ? d.selftext() : "",
                            d.fullLink(),
                            d.subreddit(),
                            Instant.ofEpochSecond(d.createdUtc())
                    ))
                    .toList();
        } catch (RestClientException e) {
            log.warn("Error al consultar Pullpush para r/{}: {}", subreddit, e.getMessage());
            return List.of();
        }
    }

    // --- Tipos internos de parseo ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PullpushResponse(
            @JsonProperty("data") List<PostData> data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PostData(
            @JsonProperty("title") String title,
            @JsonProperty("selftext") String selftext,
            @JsonProperty("full_link") String fullLink,
            @JsonProperty("subreddit") String subreddit,
            @JsonProperty("created_utc") long createdUtc
    ) {}
}
