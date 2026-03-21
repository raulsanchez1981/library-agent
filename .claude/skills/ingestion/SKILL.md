# Skill: Añadir nueva fuente de ingesta

## Interfaz principal

Toda fuente de ingesta implementa `BookSourceIngester`:

```java
public interface BookSourceIngester {
    String sourceId();                          // identificador único (ej: "reddit", "instagram")
    List<BookMention> ingest();                 // extrae menciones de la fuente
    boolean isAvailable();                      // verifica conectividad/credenciales
}
```

`BookMention` es un record inmutable:

```java
public record BookMention(
    String title,
    String author,        // puede ser null si no se detecta
    String sourceId,
    String sourceUrl,
    String rawText,
    Instant mentionedAt
) {}
```

## Patrón Strategy

Las fuentes se registran como beans de Spring y son descubiertas automáticamente por `IngestionOrchestrator`:

```java
@Service
public class RedditIngester implements BookSourceIngester {
    @Override
    public String sourceId() { return "reddit"; }
    // ...
}
```

`IngestionOrchestrator` inyecta `List<BookSourceIngester>` y delega a cada fuente activa.

## Pasos para añadir una fuente nueva

1. Crear clase en `com.libraryagent.ingestion.source.{nombre}` que implemente `BookSourceIngester`
2. Anotar con `@Service`
3. Leer credenciales desde `@Value("${ingestion.{nombre}.api-key}")` — nunca hardcodear
4. Añadir propiedad en `application.yml` bajo `ingestion.{nombre}`
5. Test unitario que mockea la llamada HTTP y verifica que `BookMention` se construye correctamente
6. Test de integración con Testcontainers o WireMock si la fuente hace llamadas externas

## Fuentes existentes

| sourceId | Clase | Mecanismo |
|---|---|---|
| `reddit` | `RedditIngester` | Reddit API v2 |
| `instagram` | `ApifyInstagramIngester` | Apify actor |
| `rss` | `RssIngester` | RSS/Atom feed parser |

## Convenciones

- El método `ingest()` nunca lanza excepciones: captura errores y retorna lista vacía con log de warning
- `isAvailable()` debe ser rápido (timeout máximo 2s)
- Usar `WebClient` (no `RestTemplate`) para llamadas HTTP
