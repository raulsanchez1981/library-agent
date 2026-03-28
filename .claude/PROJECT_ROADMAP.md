# LibraryAgent — Roadmap del proyecto

## Contexto
Aplicación personal de recomendación de libros desarrollada por Raul Sanchez
(desarrollador Java Senior). El objetivo es aprender Claude Code, subagentes,
skills, worktrees y n8n mientras se construye algo útil.

## Stack decidido
- [x] Java 21 + Spring Boot 3.x + Maven
- [x] PostgreSQL + Flyway + JPA
- [x] Redis (caché y sesiones)
- [x] Docker Compose (entorno local)
- [ ] n8n (orquestador de workflows de integración)
- [ ] Bot Telegram API
- [ ] Entrega libros: email a @kindle.com via Gmail
- [x] Fuente Reddit (Pullpush API)
- [ ] Fuente Instagram (Apify, 3-4 perfiles de bookstagramers)
- [ ] Fuente RSS blogs de reseñas
- [ ] Fuente Goodreads
- [ ] Biblioteca Kindle: API no oficial de Kindle Cloud Reader
- [ ] Dashboard: React o Next.js (fase 5)
- [ ] App móvil: React Native (fase 6, consume la API REST)
- [ ] Infraestructura: VPS Hetzner (fase 3)

## Decisiones de arquitectura tomadas
- [x] n8n gestiona flujos de integración (ingesta diaria, Telegram, entrega Kindle, resumen semanal). Spring Boot gestiona lógica de negocio compleja y API REST.
- [x] Se comunican via webhooks bidireccionales.
- [x] Subagentes con isolation: worktree para desarrollo paralelo de módulos.
- [x] Skills definen el "cómo", agentes definen el "quién y cuándo".
- [x] Instagram: se usan 3-4 perfiles fijos de bookstagramers via Apify (transcripción automática de Reels). No scraping masivo.
- [x] Kindle: envío via email @kindle.com (n8n + Gmail). Lectura de biblioteca via API no oficial en Spring Boot.
- [x] VPS: Hetzner CAX11 ARM (€4/mes, 2vCPU, 4GB RAM). Posible migración a Mini PC propio en el futuro.

## Fases del proyecto

---

### Fase 0 — COMPLETADA ✓

- [x] Suscripción Claude Pro activa
- [x] Claude Code CLI instalado en Windows
- [x] IntelliJ IDEA con plugin Claude Code
- [x] ~/.claude/CLAUDE.md global con perfil Java de Raul
- [x] GitHub conectado via SSH, repo library-agent creado
- [x] CLAUDE.md del proyecto, skills, agentes y rules creados
- [x] Esqueleto Spring Boot compilando sin errores ni warnings
- [x] Docker Compose con PostgreSQL, Redis y n8n corriendo
- [x] Flyway V1__initial_schema aplicada
- [x] Spring Boot arrancando en puerto 8080 limpio
- [x] Primer commit y push a GitHub

---

### Fase 1 — COMPLETADA ✓

- [x] Estructura de paquetes com.libraryagent completa
- [x] Entidades JPA, records DTO, interfaces de servicio
- [x] Migraciones Flyway configuradas
- [x] application.yml sin warnings
- [x] .env para desarrollo local
- [x] .claude/ con skills, agentes y rules versionado en Git

---

### Fase 2 — COMPLETADA ✓
Objetivo: primera ingesta real de datos desde Reddit.

- [x] PullpushIngester funcional (4 subreddits de fantasía/scifi)
- [x] Pipeline separado: ingesta ligera (Claude Haiku, 8:00h) + enriquecimiento nocturno (Sonnet + OL, 8:30h)
- [x] Claude Haiku para extracción de títulos por mención
- [x] Claude Sonnet para traducción al español y corrección/búsqueda de autores (batch de 10)
- [x] Sistema de confidence: HIGH (Sonnet+OL coinciden) / MEDIUM (solo Sonnet) / LOW (discrepan)
- [x] EnrichmentSource: SONNET / OL_ONLY / NONE
- [x] OpenLibrary como fallback y validador de traducciones
- [x] reEnrichAuthors(): recupera autores perdidos sin repetir enriquecimiento completo
- [x] Migraciones V1–V6 aplicadas
- [x] 44 tests unitarios pasando; 6 live tests disponibles
- [x] 145 libros procesados, 137 con autor, en primera ejecución real
- [x] Agentes funcionando con política de delegación: ingestion-agent, db-migration-agent, test-runner

---

### Fase 3 — EN CURSO
Objetivo: infraestructura completa, CI/CD y GitFlow profesional.
Motivación: todo lo que se construya a partir de aquí nace
desplegable y se trabaja con ramas y PRs desde el primer momento.

#### 3.1 — Dockerización y entornos
- [x] Dockerfile multi-stage para Spring Boot (build con Maven, runtime con JRE slim)
- [x] docker-compose.prod.yml separado del de desarrollo
- [x] Variables de entorno por entorno: .env.local / .env.prod
- [x] Ninguna credencial en el repo — todo en GitHub Secrets
- [x] Health checks en todos los servicios

#### 3.2 — GitFlow con protección de ramas
- [x] Estrategia de ramas: main → producción, develop → integración, feature/*, hotfix/*
- [x] Reglas de protección en GitHub: main y develop requieren PR + review aprobada
- [x] No push directo a main nunca
- [x] Tests deben pasar antes de permitir merge
- [x] A partir de aquí todo el desarrollo via ramas y PRs

#### 3.3 — Pipeline CI en cada PR
- [x] Build Maven + todos los tests
- [x] Análisis de vulnerabilidades en dependencias con Trivy
- [x] Detección de secretos expuestos con Gitleaks
- [x] Lint del Dockerfile
- [x] Comentario automático en la PR con el resultado
- [x] La PR no se puede mergear si alguna validación falla

#### 3.4 — Aprovisionamiento del servidor local
Infraestructura: Mini PC propio con Proxmox en red doméstica (IP dinámica, sin puertos abiertos).
- roshar (VM apps): Portainer, Nginx, Uptime Kuma, Grafana — ya instalados
- scadrial (VM BBDD): PostgreSQL / MariaDB
- Dominio propio configurado en Cloudflare (sin túnel activo aún)

- [x] Self-hosted GitHub Actions runner instalado y registrado en roshar
- [x] docker-compose.prod.yml con Spring Boot + Redis desplegado en roshar
- [x] PostgreSQL en scadrial accesible desde roshar por red local
- [x] Todo el stack corriendo: Spring Boot + Redis (roshar) + PostgreSQL (scadrial)
- [x] Acceso SSH desde máquina de desarrollo a roshar y scadrial funcionando sin problema

#### 3.5 — Pipeline CD a producción
- [x] Build imagen Docker de producción en el runner (roshar)
- [x] Deploy: runner para contenedores existentes y hace docker compose up --build
- [x] Health check post-deploy (TCP con reintentos, vuelca logs si falla)
- [ ] Notificación a Telegram: despliegue completado o fallido (Fase 5)

#### 3.6 — Acceso público con Cloudflare Tunnel
- [ ] cloudflared instalado en roshar
- [ ] Túnel activo: app.dominio.com → Nginx → Spring Boot
- [ ] Certificado TLS gestionado por Cloudflare (sin Let's Encrypt manual)

#### 3.7 — Backup y monitorización básica
- [ ] Backup automático de PostgreSQL via n8n (workflow nocturno)
- [ ] Alerta a Telegram si algún servicio cae (Uptime Kuma ya instalado)
- [ ] Métricas en Grafana (ya instalado)

#### 3.7 — Agentes y skills DevOps
- [ ] devops-agent: gestiona Dockerfile, docker-compose, variables de entorno y configuración de infraestructura
- [ ] ci-agent: especialista en GitHub Actions workflows, GHCR y pipelines de CI/CD
- [ ] security-agent: verifica que no hay credenciales en código, gestiona GitHub Secrets, revisa dependencias
- [ ] docker-skill: convenciones de Dockerfile multi-stage, docker-compose para este proyecto, health checks
- [ ] github-actions-skill: estructura de workflows, secrets de GitHub, patrones de deploy SSH, GHCR

---

### Fase 4 — PENDIENTE
Objetivo: motor de recomendaciones + dashboard web.
Desarrollado desde el principio con GitFlow: ramas feature/*, PRs con revisión.

- [ ] Motor de scoring de recomendaciones
- [ ] Perfil lector del usuario
- [ ] Dashboard web (React o Next.js)
- [ ] API REST para el dashboard

---

### Fase 5 — PENDIENTE
Objetivo: automatización completa con n8n + Telegram + Kindle.

- [ ] Workflows n8n: ingesta diaria, resumen semanal
- [ ] Bot Telegram conversacional
- [ ] Flujo descarga epub → email @kindle.com
- [ ] Webhooks Spring Boot ↔ n8n
- [ ] Notificaciones Telegram de nuevas recomendaciones

---

### Fase 6 — PENDIENTE
Objetivo: expansión de fuentes y Kindle sync.

- [ ] InstagramIngester via Apify (3-4 perfiles fijos)
- [ ] RssIngester para blogs de reseñas
- [ ] GoodreadsIngester para historial lector
- [ ] Kindle sync (API no oficial, lectura biblioteca)
- [ ] Detección libros abandonados → pregunta por Telegram

---

### Fase 7 — PENDIENTE
Objetivo: app móvil.

- [ ] React Native
- [ ] Consume la API REST existente
- [ ] Notificaciones push de nuevas recomendaciones

---

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
