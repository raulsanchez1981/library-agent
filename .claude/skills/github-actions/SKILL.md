# Skill: GitHub Actions — Convenciones del proyecto

## Estructura de workflows

El proyecto tiene dos workflows:

| Fichero | Trigger | Runner | Propósito |
|---|---|---|---|
| `ci.yml` | PR a `main`/`develop`, push a `develop` | `ubuntu-latest` | Build, tests, seguridad |
| `cd.yml` | Release publicada en GitHub | `self-hosted, linux, roshar` | Deploy a producción |

## CI — ci.yml

### Jobs actuales

```yaml
jobs:
  test:           # Build Maven + tests unitarios
  trivy:          # Vulnerabilidades HIGH/CRITICAL en deps + Dockerfile
  gitleaks:       # Secretos expuestos en todo el historial
  hadolint:       # Lint del Dockerfile
```

### Convenciones CI

- Siempre excluir `LibraryAgentApplicationTests` del CI (`-Dtest='!LibraryAgentApplicationTests'`) — requiere DB y Redis reales, se ejecutará en tests de integración (Fase 4)
- Variable de entorno `ANTHROPIC_API_KEY: test-api-key` para que Spring Boot arranque sin error en tests
- Trivy con `ignore-unfixed: true` y `exit-code: '1'` para fallar solo en vulnerabilidades con fix disponible
- Gitleaks requiere `fetch-depth: 0` para escanear todo el historial
- Hadolint sin configuración adicional — aplica reglas por defecto

### Añadir un nuevo job al CI

```yaml
  nuevo-job:
    name: Descripción en español
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Paso descriptivo
        run: comando
```

Reglas:
- `name:` siempre en español y descriptivo
- Versiones fijas para actions: `@v4`, nunca `@latest`
- Jobs independientes corren en paralelo por defecto — usar `needs:` solo si hay dependencia real

## CD — cd.yml

### Flujo completo

```yaml
on:
  release:
    types: [published]

jobs:
  deploy:
    runs-on: [self-hosted, linux, roshar]
    steps:
      - uses: actions/checkout@v4
      - name: Parar contenedores existentes
        run: |
          docker compose -p libraryagent -f docker-compose.prod.yml down --remove-orphans || true
          docker rm -f libraryagent-app libraryagent-redis 2>/dev/null || true
      - name: Build y arrancar contenedores
        run: |
          docker compose -p libraryagent -f docker-compose.prod.yml \
            --env-file /opt/libraryagent/.env.prod \
            up -d --build
      - name: Health check
        run: |
          for i in $(seq 1 18); do
            HEALTH=$(docker inspect --format='{{.State.Health.Status}}' libraryagent-app 2>/dev/null) || true
            if [ "$HEALTH" = "healthy" ]; then exit 0; fi
            sleep 10
          done
          docker logs libraryagent-app --tail 30
          exit 1
```

### Convenciones CD

- El runner self-hosted (`roshar`) tiene acceso a la red local (scadrial en 192.168.1.102)
- Las variables de entorno de producción viven en `/opt/libraryagent/.env.prod` en roshar — **nunca en el repo**
- Health check via `docker inspect` (no curl): no depende de herramientas adicionales en el contenedor
- `|| true` en el paso de parada para que no falle si los contenedores no existen
- 18 reintentos × 10 segundos = 3 minutos máximo de espera

## Secrets de GitHub Actions

Los secrets se inyectan en el workflow como variables de entorno:

```yaml
env:
  DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

Secrets actuales del proyecto:

| Secret | Job que lo usa |
|---|---|
| `ANTHROPIC_API_KEY` | cd (vía `.env.prod`) |
| `DB_NAME` | cd (vía `.env.prod`) |
| `DB_USERNAME` | cd (vía `.env.prod`) |
| `DB_PASSWORD` | cd (vía `.env.prod`) |

> Nota: en el CD actual los secrets no se inyectan directamente en el workflow sino que están en `.env.prod` del servidor. Si se migra a GHCR o a inyección directa, actualizar esta tabla.

## Comandos útiles

```bash
gh run list --limit 10           # últimas ejecuciones
gh run view {run-id}             # detalle de una ejecución
gh run watch {run-id}            # seguir en tiempo real
gh run rerun {run-id}            # relanzar
gh secret list                   # listar nombres de secrets (sin valores)
gh secret set NOMBRE             # añadir/actualizar un secret
```
