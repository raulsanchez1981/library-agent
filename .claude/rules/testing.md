# Reglas: Testing

## Estructura Given-When-Then

Todos los tests siguen la estructura GWT, marcada con comentarios cuando el bloque no es obvio:

```java
@Test
void shouldReturnEmptyListWhenNoMentionsFound() {
    // Given
    when(redditClient.fetchMentions()).thenReturn(List.of());

    // When
    List<BookMention> result = ingester.ingest();

    // Then
    assertThat(result).isEmpty();
}
```

## Nombrado de tests

Formato: `should{Resultado}When{Condicion}` en camelCase inglés:
- `shouldReturnRecommendationWhenScoreAboveThreshold`
- `shouldThrowExceptionWhenApiKeyIsInvalid`
- `shouldDeliverBookToKindleEmailSuccessfully`

## Mocks con Mockito

```java
@ExtendWith(MockitoExtension.class)
class BookAnalyzerTest {

    @Mock
    ClaudeApiClient claudeApiClient;

    @InjectMocks
    BookAnalyzerService analyzer;

    @Test
    void shouldEnrichBookWithGenresFromClaudeResponse() {
        // ...
    }
}
```

- Usar `@ExtendWith(MockitoExtension.class)`, no `MockitoAnnotations.openMocks(this)`
- No mockear clases concretas cuando se puede usar la implementación real
- Verificar interacciones solo cuando es relevante para el comportamiento (`verify`), no por defecto

## Tests de integración con Testcontainers

```java
@SpringBootTest
@Testcontainers
class BookRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

- Usar `@DataJpaTest` + Testcontainers para repositorios, no H2
- Tests de integración en paquete `...integration` o con sufijo `IT`

## Cobertura

- Mínimo **80% en lógica de negocio** (servicios, motores de scoring, ingesters)
- Infraestructura (controladores, configuración) no computa para el mínimo
- Verificar con:
  ```bash
  mvn test jacoco:report
  ```
