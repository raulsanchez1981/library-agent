# Plan de Refactoring — Backend

Fecha de análisis: 2026-04-05  
Estado del proyecto: Fase 2 completada. Pipeline Reddit→Claude→OpenLibrary operativo.  
Alcance: Solo backend Java (`src/main/java`, `src/test/java`). Frontend excluido.

---

## Principios aplicados

- **SRP** — cada clase hace una sola cosa  
- **DRY** — eliminar duplicación de lógica  
- **Consistencia de diseño** — un solo patrón por cada decisión de diseño  
- **Rendimiento correctivo** — eliminar N+1 queries y bucles O(n²) antes de que escalen  
- Orden: mayor severidad primero. No implementar una fase si la anterior introduce regressions.

---

## FASE 1 — Problemas críticos de rendimiento ✅ COMPLETADA

### 1.1 N+1 queries en `BibliotecaServiceImpl`

**Fichero**: `ingestion/service/BibliotecaServiceImpl.java:33-45`

**Problema**: El método `findAll()` carga todos los `VerifiedTitleEntity` con una query, y luego lanza una query adicional por cada título para buscar autores. Con 1.000 títulos → 1.001 queries.

```java
// Situación actual — N+1
return verifiedTitleRepository.findAllByOrderByNameAsc().stream()
    .map(vt -> {
        List<String> authors = extractedBookRepository      // ← query por elemento
            .findByVerifiedTitleAndConfidence(vt, Confidence.VERIFIED)
            ...
    })
```

**Cambios necesarios**:
1. Añadir un método en `VerifiedTitleRepository` con `@Query` y `JOIN FETCH` a autores.
2. O añadir una query JPQL que cargue en un solo viaje todo lo necesario para `VerifiedTitleDto`.
3. Alternativa: projection query que devuelva directamente `VerifiedTitleDto` sin montar entidades completas.

**Test a añadir**: `BibliotecaServiceImplTest` verificando que se lanza exactamente 1 query (con `@DataJpaTest` + Testcontainers).

---

### 1.2 Algoritmo O(n²) en `ExtractedBookAdminServiceImpl.linkUnverifiedBooks()`

**Fichero**: `ingestion/service/ExtractedBookAdminServiceImpl.java:140-172`

**Problema**: Carga todos los títulos verificados en memoria y hace una búsqueda lineal por cada libro sin verificar. Además, guarda cada libro con un `repository.save()` individual.

```java
// Situación actual — O(n²) + N INSERTs
List<VerifiedTitleEntity> todosVerificados = verifiedTitleRepository.findAll();
sinTituloVerificado.forEach(book -> todosVerificados.stream()
    .filter(vt -> vt.getName().equalsIgnoreCase(book.getTitleEs()))
    .findFirst()
    .ifPresent(vt -> {
        book.setVerifiedTitle(vt);
        repository.save(book);   // ← un INSERT por libro
    }));
```

**Cambios necesarios**:
1. Construir un `Map<String, VerifiedTitleEntity>` por nombre normalizado → búsqueda O(1).
2. Acumular los libros modificados en una lista y llamar `repository.saveAll()` una sola vez.
3. Extraer esta responsabilidad a un `BookLinkingService` separado (ver Fase 2).

**Test a añadir**: Verificar que con 500 libros sin verificar solo se lanza 1 `saveAll`.

---

## FASE 2 — Eliminar duplicación de lógica ✅ COMPLETADA

### 2.1 Método `stripMarkdownFences` duplicado en 5 clases

**Ficheros afectados**:
- `GenreEnrichmentServiceImpl.java:171`
- `CasaDelLibroScraperServiceImpl.java:141`
- `BookEnrichmentService.java:290`
- `recommendation/scoring/ClaudeScoringStrategy.java:88`
- `ingestion/extractor/BookTitleExtractor.java:56`

**Cambios necesarios**:
1. Crear clase `com.libraryagent.shared.util.MarkdownUtils` con método `static String stripFences(String raw)`.
2. Reemplazar los 5 métodos privados por llamada a `MarkdownUtils.stripFences()`.
3. La nueva clase debe tener test unitario propio con casos límite (sin fences, fences con lenguaje, cadena vacía, null).

---

### 2.2 Configuración de `AnthropicClient` en tres lugares distintos

**Ficheros afectados**:
- `RecommendationConfig.java:13-19` — Bean `@ConditionalOnProperty`
- `CasaDelLibroScraperServiceImpl.java:84-88` — Instanciado en constructor con `@Value`
- `AnthropicClaudeGateway.java:75-79` — Instanciado en constructor con `@Value`

**Problema**: Tres instancias distintas del cliente HTTP con potencial resource leak (conexiones no compartidas). Además, `RecommendationConfig` es condicional pero las otras dos instancias no lo son.

**Cambios necesarios**:
1. Definir el Bean `AnthropicClient` en una única `@Configuration`: `AnthropicConfig` (o mover a `IngestionConfig` si solo se usa en ingestion).
2. Eliminar las instanciaciones manuales en `CasaDelLibroScraperServiceImpl` y `AnthropicClaudeGateway`.
3. Inyectar el Bean vía constructor en ambas clases.
4. Asegurar `@ConditionalOnProperty` consistente en todos los lugares que lo usan.

---

### 2.3 Prompts hardcodeados en clases Java

**Ficheros afectados**:
- `AnthropicClaudeGateway.java:20-71` — 51 líneas de prompt como `static final String`
- `CasaDelLibroScraperServiceImpl.java:33-79` — 47 líneas de prompt inline

**Patrón correcto ya en uso**: `ClaudeScoringStrategy.java:110-117` carga prompts desde `src/main/resources/prompts/`.

**Cambios necesarios**:
1. Mover cada prompt a un fichero `.txt` en `src/main/resources/prompts/`.
2. Cargarlos con `ClassPathResource` + `FileCopyUtils.copyToString()` igual que en `ClaudeScoringStrategy`.
3. Nombrar los ficheros de forma descriptiva: `enrich-book-titles.txt`, `scrape-casa-del-libro.txt`.

---

## FASE 3 — Fragmentar clases con SRP violado

### 3.1 `BookEnrichmentService` — God class (325 líneas)

**Fichero**: `ingestion/service/BookEnrichmentService.java`

**Problema**: Una sola clase acumula:
- Orquestación del proceso de enriquecimiento
- Llamadas batch a Claude Sonnet
- Parsing de JSON / manejo de markdown
- Resolución de títulos en español
- Creación/búsqueda de autores
- Fallback a OpenLibrary

Además, **no tiene interface**, rompiendo el patrón usado en todos los demás servicios.

**Cambios necesarios**:

| Nueva clase | Responsabilidad | Extraído de |
|---|---|---|
| `BookEnrichmentService` (interface) | Contrato público | — |
| `BookEnrichmentServiceImpl` | Orquestación, sin lógica interna | clase actual |
| `SpanishTitleResolver` | Lógica de resolución de título en español | líneas 127-189 |
| `AuthorEnricher` | Creación y búsqueda de autores | líneas 138-152, 199-235 |
| `ClaudeResponseParser` (o `JsonResponseParser`) | Parsing de JSON + strip markdown | líneas 269-296 |

Los records internos `BookInput`, `BookLookupInput`, `SonnetEnrichment` pueden mantenerse privados en `BookEnrichmentServiceImpl` si no se reutilizan fuera.

---

### 3.2 `ExtractedBookAdminServiceImpl` — múltiples responsabilidades

**Fichero**: `ingestion/service/ExtractedBookAdminServiceImpl.java`

**Problema**: Un servicio "Admin" que hace:
- Filtering con JPA Specifications
- Update completo de libro (con enriquecimiento de géneros implícito)
- Linking de libros sin verificar a títulos verificados

**Cambios necesarios**:
1. Extraer `BookLinkingService` (interface + impl) con el método `linkUnverifiedBooks()`. Esta extracción facilita testar el algoritmo de linking en aislamiento y aplicar la optimización de la Fase 1.2.
2. `ExtractedBookAdminServiceImpl` queda con: búsqueda, update básico, delegar en `GenreEnrichmentService` y `BookLinkingService`.

---

## FASE 4 — Consolidar DTOs y eliminar confusión de naming ✅ COMPLETADA

### 4.1 DTOs de autor duplicados

**Ficheros afectados**:
- `ingestion/dto/AutorDto.java` — `id, name, photoUrl, bookCount`
- `ingestion/dto/AutorDetailDto.java` — `id, name, photoUrl, bio, openLibraryOlid, books`
- `ingestion/dto/AuthorRefDto.java` — `id, name`
- `ingestion/dto/AutorBookDto.java` — mapea `VerifiedTitleEntity` (nombre engañoso)

**Cambios necesarios**:
1. Renombrar y consolidar:
   - `AuthorRefDto` → mantener como referencia mínima (`id, name`)
   - `AutorDto` → renombrar a `AuthorSummaryDto` (`id, name, photoUrl, bookCount`)
   - `AutorDetailDto` → renombrar a `AuthorDetailDto` (ya casi correcto)
   - `AutorBookDto` → renombrar a `BookSummaryDto` o `AuthorBookDto` con nombre que refleje que mapea títulos verificados
2. Actualizar todos los usos (controllers, services, mappers).
3. Verificar con búsqueda global que no quedan referencias a los nombres viejos.

---

### 4.2 Naming "Autor" / "Author" mezclado

**Problema**: El código mezcla español e inglés para el mismo concepto:
- Entidad: `AuthorEntity` (inglés)
- Repository: `AuthorRepository` (inglés)
- Service interface: `AutorService` (español)
- Service impl: `AutorServiceImpl` (español)
- Controller: `AutorController` (español)
- DTOs: mezcla de ambos

**Cambios necesarios**:
1. Decidir un idioma para los identificadores de código (recomendación: inglés, como en el resto del stack).
2. Renombrar: `AutorService` → `AuthorService`, `AutorServiceImpl` → `AuthorServiceImpl`, `AutorController` → `AuthorController`.
3. Hacer el renombrado en un solo commit con búsqueda global para no dejar referencias rotas.

---

## FASE 5 — Unificar manejo de errores ✅ COMPLETADA

### 5.1 Excepciones inconsistentes

**Problema**: Dos patrones distintos coexisten:
- `AutorServiceImpl.java:47` → `EntityNotFoundException` (excepción JPA)
- `BibliotecaServiceImpl.java:63` → `EntityNotFoundException` (JPA)  
- `UserProfileServiceImpl.java:28` → `LibraryAgentException.notFound()` (excepción custom)

**Cambios necesarios**:
1. Verificar si existe `GlobalExceptionHandler` (`shared/exception/GlobalExceptionHandler.java`).
2. Si existe: migrar los usos de `EntityNotFoundException` a `LibraryAgentException` para que el handler centralizado los gestione uniformemente.
3. Si no existe: crearlo con `@RestControllerAdvice` que capture ambas y devuelva la estructura de error estándar definida en `api-conventions.md`.

---

## FASE 6 — Cobertura de tests en lógica crítica ✅ COMPLETADA

### 6.1 Tests que faltan o están incompletos

| Clase | Tests que faltan |
|---|---|
| `AuthorNameParser` | Casos: "King & Straub", "García Márquez, G.", null, vacío |
| `BookLinkingService` (nuevo) | Vinculación correcta, sin matches, bulk save |
| `BibliotecaServiceImpl` | Verificar número de queries (Testcontainers) |
| `MarkdownUtils` (nueva) | Fences con lenguaje, sin fences, null, cadena vacía |
| `GenreEnrichmentServiceImpl` | JSON malformado de Claude → comportamiento esperado |

### 6.2 Tests existentes que revisar

- `BookEnrichmentServiceTest` — tras el split de la Fase 3, revisar que los tests siguen siendo coherentes con las nuevas clases.
- `ExtractedBookAdminServiceTest` — añadir test para `linkUnverifiedBooks()` con el servicio extraído.

---

## FASE 7 — Limpieza menor (bajo riesgo)

### 7.1 Iteraciones múltiples en `GoogleBooksClient`

**Fichero**: `ingestion/client/GoogleBooksClient.java:109-187`

**Problema**: 6 bucles secuenciales sobre la misma lista `items` con criterios acumulativos.

**Cambios necesarios**: Refactorizar a una única pasada que asigne una puntuación/prioridad a cada item y devuelva el de mayor score. Más legible y eficiente.

---

### 7.2 Wrapper innecesario en `GoogleBooksEnrichServiceImpl`

**Fichero**: `ingestion/service/GoogleBooksEnrichServiceImpl.java:18-22`

**Problema**: El servicio es un delegado puro sin ninguna lógica adicional.

**Decisión**: Evaluar si la interface `GoogleBooksEnrichService` aporta valor (testabilidad, sustitución). Si no hay tests que la mockeen ni planes de sustituirla, eliminar la capa y llamar directamente a `GoogleBooksClient` desde quien lo consume.

---

### 7.3 URLs base de APIs en `application.yml`

**Ficheros afectados**:
- `GoogleBooksClient.java:29` — `BASE_URL` hardcodeada
- `RestClientOpenLibraryClient.java:22` — `BASE_URL` hardcodeada

**Cambios necesarios**: Mover a `application.yml` con prefijo de configuración (`google.books.base-url`, `open-library.base-url`) e inyectar con `@Value`.

---

## Orden de ejecución recomendado

```
Fase 1 (rendimiento)        ← Máximo impacto, sin dependencias
Fase 2 (duplicación)        ← Prerrequisito para Fase 3
Fase 3 (SRP)                ← Depende de Fase 2 (MarkdownUtils, AnthropicClient)
Fase 4 (DTOs/naming)        ← Independiente, puede ir paralela a Fase 3
Fase 5 (errores)            ← Independiente
Fase 6 (tests)              ← Después de cada fase, no al final
Fase 7 (limpieza menor)     ← Última, menor riesgo
```

Ejecutar `mvn test` (o invocar `test-runner`) al finalizar cada fase para detectar regressions antes de avanzar.

---

## Ficheros que NO tocar en este refactoring

- Todo lo que esté en `src/main/resources/db/migration/` → gestión exclusiva de `db-migration-agent`
- `SecurityConfig.java` — cambios de seguridad requieren análisis separado
- Entidades JPA (`*Entity.java`) — cambios en entidades implican migraciones Flyway; tratarlos por separado
- `docker-compose*.yml` — fuera del alcance de este refactoring

---

## Métricas de éxito

| Métrica | Situación actual (estimada) | Objetivo tras refactoring |
|---|---|---|
| Queries en `BibliotecaService.findAll()` | N+1 | 1 o 2 |
| Copias de `stripMarkdownFences` | 5 | 0 (eliminadas, 1 en `MarkdownUtils`) |
| Instancias de `AnthropicClient` | 3 | 1 (bean singleton) |
| Líneas de `BookEnrichmentService` | ~325 | <100 (orquestador puro) |
| DTOs de autor | 4 | 3 semánticamente claros |
| Cobertura en `AuthorNameParser` | 0% | >90% |
