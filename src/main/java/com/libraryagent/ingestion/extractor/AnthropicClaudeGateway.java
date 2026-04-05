package com.libraryagent.ingestion.extractor;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implementación de ClaudeGateway que llama a la API de Anthropic via OkHttp.
 * Extracción: Claude Haiku (rápido y económico, una llamada por RawMention).
 * Enriquecimiento batch: Claude Sonnet (mejor conocimiento de traducciones).
 */
public class AnthropicClaudeGateway implements ClaudeGateway {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClaudeGateway.class);

    private final AnthropicClient client;
    private final String extractPrompt;
    private final String lookupAuthorsPrompt;
    private final String inferGenresPrompt;
    private final String enrichPrompt;

    public AnthropicClaudeGateway(AnthropicClient client) {
        this.client = client;
        this.extractPrompt = loadPrompt("extract-books.txt");
        this.lookupAuthorsPrompt = loadPrompt("lookup-authors.txt");
        this.inferGenresPrompt = loadPrompt("infer-genres.txt");
        this.enrichPrompt = loadPrompt("enrich-books.txt");
    }

    @Override
    public String extractBooksJson(String mentionText) {
        return call(Model.CLAUDE_HAIKU_4_5_20251001, extractPrompt.formatted(mentionText), 512L);
    }

    @Override
    public String enrichBooksBatchJson(String booksJson) {
        return call(Model.of("claude-sonnet-4-6"), enrichPrompt.formatted(booksJson), 1024L);
    }

    @Override
    public String lookupAuthorsBatchJson(String booksJson) {
        return call(Model.of("claude-sonnet-4-6"), lookupAuthorsPrompt.formatted(booksJson), 512L);
    }

    @Override
    public String inferGenresBatchJson(String booksJson) {
        return call(Model.CLAUDE_HAIKU_4_5_20251001, inferGenresPrompt.formatted(booksJson), 1024L);
    }

    private String call(Model model, String prompt, long maxTokens) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .addUserMessage(prompt)
                .build();

        return client.messages().create(params)
                .content()
                .stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .findFirst()
                .orElse("[]");
    }

    private static String loadPrompt(String filename) {
        try {
            return new ClassPathResource("prompts/" + filename)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se puede cargar prompts/" + filename, e);
        }
    }
}
