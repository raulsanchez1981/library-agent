package com.libraryagent.ingestion.extractor;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación de ClaudeGateway que llama a la API de Anthropic via OkHttp.
 */
public class AnthropicClaudeGateway implements ClaudeGateway {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClaudeGateway.class);

    private static final String PROMPT_TEMPLATE = """
            Extrae todos los títulos de libros mencionados en el siguiente \
            texto. Devuelve SOLO un array JSON con los títulos exactos, \
            sin explicaciones ni texto adicional. Si no hay títulos de \
            libros, devuelve [].

            Texto: %s""";

    private final AnthropicClient client;

    public AnthropicClaudeGateway(String apiKey) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String extractTitlesJson(String mentionText) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5_20251001)
                .maxTokens(512L)
                .addUserMessage(PROMPT_TEMPLATE.formatted(mentionText))
                .build();

        return client.messages().create(params)
                .content()
                .stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .findFirst()
                .orElse("[]");
    }
}
