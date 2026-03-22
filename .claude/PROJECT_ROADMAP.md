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
- Fuentes de ingesta: Reddit (API oficial), Instagram (Apify, 3-4 perfiles
  de bookstagramers), RSS blogs de reseñas, Goodreads
- Biblioteca Kindle: API no oficial de Kindle Cloud Reader
- Dashboard: React o Next.js (fase 5)
- App móvil: React Native (fase 5, consume la API REST)
- Infraestructura: Docker Compose local

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
- BookTitleExtractor con Claude Haiku (extracción ligera por mención)
- BookEnrichmentService con Claude Sonnet + OpenLibrary (enriquecimiento batch)
- Sistema de confidence scoring: HIGH (Sonnet+OL coinciden) / MEDIUM (solo Sonnet) / LOW (Sonnet y OL discrepan) / NONE (sin traducción)
- EnrichmentSource simplificado: SONNET / OL_ONLY / NONE
- Pipeline separado en dos fases: ingesta (8:00h) + enriquecimiento (8:30h) via schedulers
- Migraciones V1–V6 aplicadas correctamente
- 41 tests unitarios pasando, 5 live tests disponibles
- Agentes delegando correctamente: ingestion-agent, db-migration-agent, test-runner
- 145 libros procesados en primera ejecución real (97 menciones)

### Fase 3 — PENDIENTE
Objetivo: motor de recomendaciones + dashboard web básico.
- UserProfile con historial y preferencias
- ScoringEngine usando Claude API
- API REST para consultar recomendaciones
- Dashboard web básico (React/Next.js o Thymeleaf temporal)

### Fase 4 — PENDIENTE
Objetivo: automatización completa con n8n + Telegram + Kindle.
- n8n instalado y configurado (ya corre en Docker)
- Workflow ingesta diaria (cron 8am)
- Bot Telegram conversacional
- Flujo descarga epub → email @kindle.com
- Resumen semanal por Gmail
- Webhook Spring Boot ↔ n8n

### Fase 5 — PENDIENTE
Objetivo: expansión de fuentes y canales.
- InstagramIngester via Apify (3-4 perfiles fijos)
- RssIngester para blogs de reseñas
- GoodreadsIngester para historial lector
- Kindle sync (API no oficial, lectura biblioteca)
- App móvil React Native
- Detección libros abandonados → pregunta por Telegram

## Lo que Claude Code debe saber siempre
- Este proyecto es para uso personal de Raul, no producción empresarial
- El objetivo secundario es aprender el ecosistema Claude Code completo
- Raul es Senior Java, no necesita explicaciones básicas
- Siempre en español en las respuestas
- Commits en Conventional Commits español
- Nunca hacer commit sin confirmación explícita de Raul