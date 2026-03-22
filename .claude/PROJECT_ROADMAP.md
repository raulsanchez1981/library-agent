# LibraryAgent — Roadmap del proyecto

## Contexto
Aplicación personal de recomendación de libros desarrollada por Raul Sanchez
(desarrollador Java Senior). El objetivo es aprender Claude Code, subagentes,
skills, worktrees y n8n mientras se construye algo útil.

## Stack decidido
- Backend: Java 21 + Spring Boot 3.x + Maven
- Base de datos: PostgreSQL + Flyway + JPA
- Caché: Redis
- Orquestador de workflows: n8n (self-hosted en Docker)
- Bot: Telegram API
- Entrega libros: email a @kindle.com via Gmail
- Fuentes de ingesta: Reddit (Pullpush API), Instagram (Apify, 3-4 perfiles
  de bookstagramers), RSS blogs de reseñas, Goodreads
- Biblioteca Kindle: API no oficial de Kindle Cloud Reader
- Dashboard: React o Next.js (fase 5)
- App móvil: React Native (fase 6, consume la API REST)
- Infraestructura: Docker Compose → VPS Hetzner (fase 3)

## Decisiones de arquitectura tomadas
- n8n gestiona flujos de integración (ingesta diaria, Telegram, entrega Kindle,
  resumen semanal). Spring Boot gestiona lógica de negocio compleja y API REST.
- Se comunican via webhooks bidireccionales.
- Subagentes con isolation: worktree para desarrollo paralelo de módulos.
- Skills definen el "cómo", agentes definen el "quién y cuándo".
- Instagram: se usan 3-4 perfiles fijos de bookstagramers via Apify
  (transcripción automática de Reels). No scraping masivo.
- Kindle: envío via email @kindle.com (n8n + Gmail).
  Lectura de biblioteca via API no oficial en Spring Boot.
- VPS: Hetzner CAX11 ARM (€4/mes, 2vCPU, 4GB RAM).
  Posible migración a Mini PC propio en el futuro.

## Fases del proyecto

### Fase 0 — COMPLETADA
- Suscripción Claude Pro activa
- Claude Code CLI instalado en Windows
- IntelliJ IDEA con plugin Claude Code
- ~/.claude/CLAUDE.md global con perfil Java de Raul
- GitHub conectado via SSH, repo library-agent creado
- CLAUDE.md del proyecto, skills, agentes y rules creados
- Esqueleto Spring Boot compilando sin errores ni warnings
- Docker Compose con PostgreSQL, Redis y n8n corriendo
- Flyway V1__initial_schema aplicada
- Spring Boot arrancando en puerto 8080 limpio
- Primer commit y push a GitHub

### Fase 1 — COMPLETADA
- Estructura de paquetes com.libraryagent completa
- Entidades JPA, records DTO, interfaces de servicio
- Migraciones Flyway configuradas
- application.yml sin warnings
- .env para desarrollo local
- .claude/ con skills, agentes y rules versionado en Git

### Fase 2 — COMPLETADA
Objetivo: primera ingesta real de datos desde Reddit.
- PullpushIngester funcional (4 subreddits de fantasía/scifi)
- Pipeline separado: ingesta ligera (Claude Haiku, 8:00h) +
  enriquecimiento nocturno (Sonnet + OL, 8:30h)
- Claude Haiku para extracción de títulos por mención
- Claude Sonnet para traducción al español y corrección/búsqueda
  de autores (batch de 10)
- Sistema de confidence: HIGH (Sonnet+OL coinciden) /
  MEDIUM (solo Sonnet) / LOW (discrepan)
- EnrichmentSource: SONNET / OL_ONLY / NONE
- OpenLibrary como fallback y validador de traducciones
- reEnrichAuthors(): recupera autores perdidos sin repetir
  enriquecimiento completo
- Migraciones V1–V6 aplicadas; 44 tests unitarios pasando;
  6 live tests disponibles
- 145 libros procesados, 137 con autor, en primera ejecución real
- Agentes funcionando con política de delegación:
  ingestion-agent, db-migration-agent, test-runner

### Fase 3 — EN CURSO
Objetivo: infraestructura completa, CI/CD y GitFlow profesional.
Motivación: todo lo que se construya a partir de aquí nace
desplegable y se trabaja con ramas y PRs desde el primer momento.

#### 3.1 — Dockerización y entornos
- Dockerfile multi-stage para Spring Boot
  (build con Maven, runtime con JRE slim)
- docker-compose.prod.yml separado del de desarrollo
- Variables de entorno por entorno: .env.local / .env.prod
- Ninguna credencial en el repo — todo en GitHub Secrets
- Health checks en todos los servicios

#### 3.2 — GitFlow con protección de ramas
- Estrategia de ramas:
  main → producción (solo via PR aprobada)
  develop → integración continua
  feature/* → nuevas funcionalidades
  hotfix/* → correcciones urgentes a producción
- Reglas de protección en GitHub:
  main y develop requieren PR + review aprobada
  No push directo a main nunca
  Tests deben pasar antes de permitir merge
- A partir de aquí todo el desarrollo via ramas y PRs

#### 3.3 — Pipeline CI en cada PR
GitHub Actions ejecuta automáticamente en cada PR:
- Build Maven + todos los tests
- Análisis de vulnerabilidades en dependencias con Trivy
- Detección de secretos expuestos con Gitleaks
- Lint del Dockerfile
- Comentario automático en la PR con el resultado
- La PR no se puede mergear si alguna validación falla

#### 3.4 — Aprovisionamiento del VPS
- Hetzner CAX11 ARM (€4/mes, 2vCPU, 4GB RAM)
- Script de bootstrap: instala Docker, configura firewall UFW,
  crea usuario de deploy, configura acceso SSH por clave
- docker-compose.prod.yml desplegado en el VPS
- Todo el stack corriendo en producción:
  Spring Boot + PostgreSQL + Redis + n8n

#### 3.5 — Pipeline CD a producción
GitHub Actions en merge a main:
- Solo cuando Raul aprueba manualmente (manual approval)
- Build imagen Docker de producción
- Push a GitHub Container Registry (GHCR) con tag semver
- Deploy via SSH al VPS
- Health check post-deploy
- Notificación a Telegram: despliegue completado o fallido

#### 3.6 — Backup y monitorización básica
- Backup automático de PostgreSQL via n8n (workflow nocturno)
- Alerta a Telegram si algún servicio cae
- Logs centralizados accesibles

#### 3.7 — Agentes y skills DevOps
Nuevos agentes en .claude/agents/:
- devops-agent: gestiona Dockerfile, docker-compose,
  variables de entorno y configuración de infraestructura.
  DEBE invocarse cuando se toque cualquier fichero de infra.
- ci-agent: especialista en GitHub Actions workflows,
  GHCR y pipelines de CI/CD.
  DEBE invocarse cuando se toque .github/workflows/.
- security-agent: verifica que no hay credenciales en código,
  gestiona GitHub Secrets, revisa dependencias.
  DEBE invocarse antes de cualquier commit que toque
  configuración o variables de entorno.

Nuevos skills en .claude/skills/:
- docker-skill: convenciones de Dockerfile multi-stage,
  docker-compose para este proyecto, health checks
- github-actions-skill: estructura de workflows, secrets
  de GitHub, patrones de deploy SSH, GHCR

### Fase 4 — PENDIENTE
Objetivo: motor de recomendaciones + dashboard web.
Desarrollado desde el principio con GitFlow: ramas feature/*,
PRs con validaciones automáticas, deploy automático al aprobar.
- UserProfile con historial y preferencias
- ScoringEngine usando Claude API
- API REST para consultar recomendaciones
- Dashboard web (React/Next.js o Thymeleaf temporal)
- recommendation-agent activo para este módulo

### Fase 5 — PENDIENTE
Objetivo: automatización completa con n8n + Telegram + Kindle.
- Workflows n8n: ingesta diaria, resumen semanal
- Bot Telegram conversacional
- Flujo descarga epub → email @kindle.com
- Webhooks Spring Boot ↔ n8n
- Notificaciones Telegram de nuevas recomendaciones

### Fase 6 — PENDIENTE
Objetivo: expansión de fuentes y Kindle sync.
- InstagramIngester via Apify (3-4 perfiles fijos)
- RssIngester para blogs de reseñas
- GoodreadsIngester para historial lector
- Kindle sync (API no oficial, lectura biblioteca)
- Detección libros abandonados → pregunta por Telegram

### Fase 7 — PENDIENTE
Objetivo: app móvil.
- React Native
- Consume la API REST existente
- Notificaciones push de nuevas recomendaciones

## Lo que Claude Code debe saber siempre
- Este proyecto es para uso personal de Raul, no producción empresarial
- El objetivo secundario es aprender el ecosistema Claude Code completo
- Raul es Senior Java, no necesita explicaciones básicas
- Siempre en español en las respuestas
- Commits en Conventional Commits español
- Nunca hacer commit sin confirmación explícita de Raul
- Fase actual: Fase 3 — Infraestructura y CI/CD
- Todo el desarrollo a partir de ahora via ramas feature/* y PRs
- Nunca push directo a main ni a develop
