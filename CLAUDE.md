# LibraryAgent

Aplicación personal de recomendación de libros.

## Stack técnico

- Java 21 + Spring Boot 3.x + Maven
- PostgreSQL (base de datos principal)
- Redis (caché y sesiones)
- Flyway (migraciones)
- Docker Compose (entorno local)
- n8n (orquestador de workflows de integración)

## Módulos

| Módulo | Responsabilidad |
|---|---|
| `ingestion` | Recoge menciones de libros desde fuentes externas (Reddit, Instagram via Apify, RSS) |
| `recommendation` | Motor de scoring que analiza libros contra el perfil del usuario usando Claude API |
| `kindle-sync` | Sincronización con biblioteca Kindle via API no oficial |
| `notification` | Entrega de libros al Kindle via email y bot de Telegram |
| `user-profile` | Gestión del perfil lector, historial y preferencias |

## Estructura de paquetes

```
com.libraryagent.{modulo}
```

## Comandos

```bash
mvn clean install     # build completo
mvn test              # ejecutar tests
mvn spring-boot:run   # arrancar la aplicación (puerto 8080)
```

## Convenciones de código

- Interfaces para todos los servicios
- Records para DTOs inmutables
- Sealed classes para jerarquías de tipos
- Repository pattern para acceso a datos
- Nunca SQL directo: siempre JPA o QueryDSL
- Tests unitarios obligatorios para toda lógica de negocio
- Tests de integración con Testcontainers para repositorios

## Reglas de comportamiento (CRÍTICO)

- **Parada dura**: tras 3 tool calls sin solución clara → parar y preguntar. Nunca investigar indefinidamente.
- **Errores secundarios**: si aparece un error no relacionado con la tarea → mencionarlo y preguntar, no investigarlo.
- **Subdirectorios**: leer AGENTS.md o CLAUDE.md del subdirectorio ANTES de tocar cualquier fichero dentro.
- **Sesiones largas**: si la sesión lleva muchos intercambios, la calidad decrece. El usuario puede usar `/clear` para resetear.

## Política de uso de agentes
Delegar en el agente correspondiente cuando la tarea encaje con su descripción.

Orden de prioridad:
1. db-migration-agent → cualquier fichero .sql nuevo o modificado
2. ingestion-agent → cualquier clase en paquete ingestion/ con lógica nueva o refactoring
3. recommendation-agent → cualquier clase en paquete recommendation/
4. test-runner → **solo** al finalizar cambios de lógica de negocio real (.java con lógica nueva)

**test-runner NO se invoca** para: añadir mocks en tests, añadir imports, cambiar strings literales,
fixes de 1-3 líneas trivialmente correctos. En esos casos verificar con Grep y confiar en análisis estático.

**Scrapers HTML**: antes de escribir cualquier scraper, hacer WebFetch sobre la URL real y verificar
que los selectores existen en el HTML estático. Si el site usa Svelte/React/Vue, buscar JSON-LD
y meta tags (siempre en HTML estático). Ver `.claude/rules/agent-efficiency.md`.

## Estado del proyecto
Lee siempre .claude/PROJECT_ROADMAP.md al inicio de cada sesión
para conocer las fases completadas y el trabajo pendiente.
Fase actual: Fase 2 — pipeline completo implementado y probado (Pullpush→Claude→OpenLibrary, IngestionService operativo, tests unitarios completos).
Siguiente: persistencia en PostgreSQL, tests de integración con Testcontainers y scheduler cron diario.
