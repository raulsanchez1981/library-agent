package com.libraryagent.ingestion.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.PullpushProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BookTitleExtractorConfig {

    @Bean
    ClaudeGateway claudeGateway(@Value("${anthropic.api-key}") String apiKey) {
        return new AnthropicClaudeGateway(apiKey);
    }

    @Bean
    BookTitleExtractor bookTitleExtractor(ClaudeGateway claudeGateway, ObjectMapper objectMapper) {
        return new BookTitleExtractor(claudeGateway, objectMapper);
    }
}
