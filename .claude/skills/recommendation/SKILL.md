# Skill: Motor de recomendación y scoring

## Flujo general

```
BookMention → BookAnalyzer → ScoringEngine → RecommendationResult
                   ↑                ↑
             Claude API        UserProfile
```

1. `BookAnalyzer` enriquece la mención con metadata (género, temas, idioma) llamando a Claude API
2. `ScoringEngine` compara el libro enriquecido contra `UserProfile` y produce un `RecommendationScore`
3. Si el score supera el umbral configurado, el libro se encola para notificación

## Llamada a Claude API

El prompt se construye en `RecommendationPromptBuilder`. Formato esperado en la respuesta:

```json
{
  "score": 0.87,
  "reasons": ["coincide con interés en ciencia ficción dura", "autor favorito del usuario"],
  "genres": ["science-fiction", "hard-sf"],
  "themes": ["space-exploration", "AI"],
  "readingTimeHours": 8,
  "confidence": "high"
}
```

Claude debe responder **solo** con el JSON anterior, sin texto adicional. El prompt incluye la instrucción `Respond only with valid JSON`.

## UserProfile

```java
public record UserProfile(
    List<String> favoriteGenres,
    List<String> favoriteAuthors,
    List<String> avoidedGenres,
    List<String> readBooks,          // ISBNs o títulos normalizados
    int preferredReadingTimeMaxHours
) {}
```

## ScoringEngine

```java
public interface ScoringEngine {
    RecommendationScore score(EnrichedBook book, UserProfile profile);
}
```

`RecommendationScore` es un record con `double value` (0.0–1.0) y `List<String> reasons`.

## Umbral de recomendación

Configurable en `application.yml`:
```yaml
recommendation:
  score-threshold: 0.75
  claude-model: claude-opus-4-6
```

## Convenciones

- El prompt completo se externaliza en `src/main/resources/prompts/recommendation.txt`
- Parsear la respuesta de Claude con Jackson; si falla el parse, loguear y descartar (no lanzar excepción)
- Tests unitarios mockean `ClaudeApiClient`, nunca llaman a la API real
- Test de integración usa una respuesta JSON fija almacenada en `src/test/resources/fixtures/`
