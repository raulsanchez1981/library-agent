package com.libraryagent.ingestion.extractor;

import com.anthropic.client.AnthropicClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BookTitleExtractorConfig {

    @Bean
    ClaudeGateway claudeGateway(AnthropicClient anthropicClient) {
        return new AnthropicClaudeGateway(anthropicClient);
    }

    @Bean
    BookTitleExtractor bookTitleExtractor(ClaudeGateway claudeGateway, ObjectMapper objectMapper) {
        return new BookTitleExtractor(claudeGateway, objectMapper);
    }
}
