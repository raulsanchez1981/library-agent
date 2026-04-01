package com.libraryagent.recommendation.scoring;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryagent.ingestion.model.ExtractedBookEntity;
import com.libraryagent.recommendation.model.ScoringResult;
import com.libraryagent.recommendation.model.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "anthropic.api-key")
public final class ClaudeScoringStrategy implements BookScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(ClaudeScoringStrategy.class);

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;
    private final String claudeModel;
    private final String promptTemplate;

    public ClaudeScoringStrategy(
            AnthropicClient anthropicClient,
            ObjectMapper objectMapper,
            @Value("${recommendation.claude-model:claude-sonnet-4-6}") String claudeModel) {
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
        this.claudeModel = claudeModel;
        this.promptTemplate = loadPromptTemplate();
    }

    @Override
    public ScoringResult score(ExtractedBookEntity book, UserPreferences preferences) {
        String title = resolveTitle(book);
        String author = resolveAuthor(book);
        String prompt = buildPrompt(title, author, preferences);

        String rawResponse = callClaude(prompt);
        return parseResponse(rawResponse);
    }

    private String buildPrompt(String title, String author, UserPreferences preferences) {
        String genres = String.join(", ", preferences.favoriteGenres());
        String authors = String.join(", ", preferences.favoriteAuthors());
        String readBooks = String.join(", ", preferences.completedBookTitles());
        return promptTemplate.formatted(title, author, preferences.preferredLanguage(), genres, authors, readBooks);
    }

    private String callClaude(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of(claudeModel))
                .maxTokens(256L)
                .addUserMessage(prompt)
                .build();

        return anthropicClient.messages().create(params)
                .content()
                .stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude no devolvió respuesta para el scoring"));
    }

    private ScoringResult parseResponse(String rawResponse) {
        try {
            String clean = stripMarkdownFences(rawResponse);
            JsonNode node = objectMapper.readTree(clean);
            int score = node.path("score").asInt();
            String reasoning = node.path("reasoning").asText();
            return new ScoringResult(Math.max(0, Math.min(100, score)), reasoning);
        } catch (Exception e) {
            throw new IllegalStateException("Error al parsear respuesta de Claude: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownFences(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("```json\\s*", "").replaceAll("```", "").strip();
        }
        return trimmed;
    }

    private String resolveTitle(ExtractedBookEntity book) {
        if (book.getTitleEs() != null && !book.getTitleEs().isBlank()) {
            return book.getTitleEs();
        }
        return book.getTitle();
    }

    private String resolveAuthor(ExtractedBookEntity book) {
        if (book.getAuthorCorrected() != null && !book.getAuthorCorrected().isBlank()) {
            return book.getAuthorCorrected();
        }
        return book.getAuthor() != null ? book.getAuthor() : "";
    }

    private static String loadPromptTemplate() {
        try {
            return new ClassPathResource("prompts/recommendation.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se puede cargar prompts/recommendation.txt", e);
        }
    }
}
