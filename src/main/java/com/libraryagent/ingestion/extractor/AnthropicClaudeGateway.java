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
 * Extracción: Claude Haiku (rápido y económico, una llamada por RawMention).
 * Enriquecimiento batch: Claude Sonnet (mejor conocimiento de traducciones).
 */
public class AnthropicClaudeGateway implements ClaudeGateway {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClaudeGateway.class);

    private static final String EXTRACT_PROMPT = """
            Extrae todos los libros mencionados en el siguiente texto.
            Devuelve SOLO un array JSON donde cada objeto tiene:
            - title: título del libro tal como se menciona
            - author: autor si se menciona explícitamente en el texto, null si no
            - isSaga: true si es una saga o serie completa, false si es un libro individual

            Si no hay libros, devuelve [].

            Texto: %s""";

    private static final String LOOKUP_AUTHORS_PROMPT = """
            Eres un experto en literatura mundial. Para cada libro de esta lista, \
            proporciona el nombre del autor principal si lo conoces.
            Devuelve en el mismo orden un JSON array donde cada objeto tiene:
            - author: nombre completo del autor (null si no conoces el libro o no tienes certeza)

            Lista: %s

            Responde SOLO con el JSON array, sin explicaciones.""";

    private static final String INFER_GENRES_PROMPT = """
            Eres un experto en literatura. Para cada libro de esta lista, \
            infiere entre 2 y 4 géneros literarios específicos en español. \
            Usa la sinopsis cuando esté disponible para ser más preciso, \
            aunque no conozcas el título directamente.
            Géneros válidos (no exhaustivos): Fantasía épica, Fantasía juvenil, \
            Magia y brujería, Ciencia ficción, Distopía, Space opera, \
            Novela negra, Thriller, Misterio, Terror, Romance, Romance fantástico, \
            Aventura, Aventura juvenil, Historia, Novela histórica, Biografía, \
            Ensayo, Humor, Drama, Contemporáneo, Clásico.
            Devuelve en el mismo orden un JSON array donde cada objeto tiene:
            - genres: array de strings con los géneros inferidos (nunca vacío)

            Lista: %s

            Responde SOLO con el JSON array, sin explicaciones.""";

    private static final String ENRICH_PROMPT = """
            Eres un experto en literatura mundial. Para cada libro de esta lista \
            devuelve en el mismo orden un JSON array donde cada objeto tiene:
            - titleEs: título oficial en español más conocido (null si el libro \
            no tiene traducción española publicada conocida)
            - authorCorrected: nombre correcto del autor si el proporcionado tiene \
            errores tipográficos o de formato, o null si es correcto o no hay autor
            - isSaga: true si es una saga o serie completa, false si es un libro individual

            Sé conservador: devuelve null en titleEs si no estás seguro.

            Lista: %s

            Responde SOLO con el JSON array, sin explicaciones.""";

    private final AnthropicClient client;

    public AnthropicClaudeGateway(String apiKey) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String extractBooksJson(String mentionText) {
        return call(Model.CLAUDE_HAIKU_4_5_20251001, EXTRACT_PROMPT.formatted(mentionText), 512L);
    }

    @Override
    public String enrichBooksBatchJson(String booksJson) {
        return call(Model.of("claude-sonnet-4-6"), ENRICH_PROMPT.formatted(booksJson), 1024L);
    }

    @Override
    public String lookupAuthorsBatchJson(String booksJson) {
        return call(Model.of("claude-sonnet-4-6"), LOOKUP_AUTHORS_PROMPT.formatted(booksJson), 512L);
    }

    @Override
    public String inferGenresBatchJson(String booksJson) {
        return call(Model.CLAUDE_HAIKU_4_5_20251001, INFER_GENRES_PROMPT.formatted(booksJson), 1024L);
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
}
