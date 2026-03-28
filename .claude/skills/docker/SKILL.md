# Skill: Docker — Convenciones del proyecto

## Dockerfile multi-stage

El Dockerfile sigue el patrón de dos stages:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q        # capa cacheada si pom.xml no cambia
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Usuario no-root obligatorio
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Reglas del Dockerfile
- Siempre separar `COPY pom.xml` y `COPY src` para aprovechar caché de dependencias
- Usar `-q` en comandos Maven para logs limpios
- Usuario no-root obligatorio en runtime
- `-XX:+UseContainerSupport` y `-XX:MaxRAMPercentage=75.0` siempre en el ENTRYPOINT
- `-DskipTests` en el build del Dockerfile (los tests corren en CI)

## docker-compose.yml (desarrollo local)

Incluye: PostgreSQL, Redis, n8n. La app Spring Boot se arranca con `mvn spring-boot:run` localmente.

```yaml
services:
  postgres:
    image: postgres:16
    container_name: libraryagent-postgres
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5
```

## docker-compose.prod.yml (producción en roshar)

Solo incluye: app Spring Boot + Redis. PostgreSQL vive en scadrial (192.168.1.102).

```yaml
services:
  app:
    build: .
    container_name: libraryagent-app
    restart: unless-stopped
    environment:
      DB_HOST: 192.168.1.102
      DB_PORT: 5432
      # resto de variables desde .env.prod
    ports:
      - "8080:8080"
    depends_on:
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "timeout 5 bash -c 'cat < /dev/null > /dev/tcp/localhost/8080' || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    networks:
      - libraryagent-net
```

## Health checks

Reglas:
- **Todo servicio** en producción debe tener health check
- `start_period` generoso para Spring Boot (mínimo 60s por el arranque de JVM + Flyway)
- PostgreSQL: `pg_isready`
- Redis: `redis-cli ping`
- Spring Boot: TCP check al puerto 8080 (no depende de curl instalado en el contenedor)

## Variables de entorno

- Nunca hardcodear valores en docker-compose — siempre `${VARIABLE}`
- En producción, las variables vienen de `/opt/libraryagent/.env.prod` pasado con `--env-file`
- La IP de scadrial (192.168.1.102) es la única excepción: es infraestructura fija, no un secret

## Redes

- Producción usa red bridge `libraryagent-net` explícita
- app y redis en la misma red; PostgreSQL en servidor externo (scadrial)

## Comandos habituales

```bash
# Producción (ejecutar en roshar)
docker compose -p libraryagent -f docker-compose.prod.yml --env-file /opt/libraryagent/.env.prod up -d --build
docker compose -p libraryagent -f docker-compose.prod.yml down --remove-orphans

# Debug
docker inspect --format='{{.State.Health.Status}}' libraryagent-app
docker logs libraryagent-app --tail 50 -f
```
