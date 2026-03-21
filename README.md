# LibraryAgent

Aplicación personal de recomendación de libros. Recoge menciones desde Reddit, Instagram y RSS, las analiza con Claude API y entrega los más relevantes al Kindle.

## Requisitos

- Java 21
- Maven 3.9+
- Docker y Docker Compose

## Arrancar el entorno local

```bash
# 1. Levantar infraestructura (PostgreSQL, Redis, n8n)
docker compose up -d

# 2. Copiar y configurar variables de entorno
cp .env.example .env
# editar .env con tus valores

# 3. Arrancar la aplicación
mvn spring-boot:run
```

La API estará disponible en `http://localhost:8080`.
Swagger UI en `http://localhost:8080/swagger-ui.html`.
n8n en `http://localhost:5678`.

## Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_HOST` | Host PostgreSQL | `localhost` |
| `DB_PORT` | Puerto PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `libraryagent` |
| `DB_USERNAME` | Usuario PostgreSQL | `libraryagent` |
| `DB_PASSWORD` | Contraseña PostgreSQL | `changeme` |
| `REDIS_HOST` | Host Redis | `localhost` |
| `REDIS_PORT` | Puerto Redis | `6379` |
| `CLAUDE_API_KEY` | API key de Anthropic | `sk-ant-...` |
| `KINDLE_SENDER_EMAIL` | Email aprobado en Amazon | `tu@gmail.com` |
| `N8N_USER` | Usuario n8n | `admin` |
| `N8N_PASSWORD` | Contraseña n8n | `changeme` |

## Comandos útiles

```bash
mvn clean install          # build completo con tests
mvn test                   # ejecutar todos los tests
mvn test -pl ingestion     # tests de un módulo concreto
mvn flyway:info            # estado de migraciones
docker compose down -v     # parar y borrar volúmenes
```

## Módulos

- **ingestion** — fuentes de datos (Reddit, Instagram via Apify, RSS)
- **recommendation** — motor de scoring con Claude API
- **kindle-sync** — sincronización con biblioteca Kindle
- **notification** — entrega via email y Telegram
- **user-profile** — perfil lector, historial y preferencias
