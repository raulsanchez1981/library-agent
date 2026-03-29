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
- [x] Health check post-deploy via docker inspect (healthy/starting), vuelca logs si falla
- [x] Spring Boot Actuator operativo en producción: /actuator/health con detalle de DB, Redis y Flyway
- [ ] Notificación a Telegram: despliegue completado o fallido (Fase 5)

#### 3.6 — Acceso público con Cloudflare Tunnel ✓
- [x] cloudflared instalado en roshar (contenedor Docker)
- [x] Túnel activo: subdominio.mistborn.cv → Cloudflare Tunnel → NPM (Nginx Proxy Manager) → app
- [x] Certificado TLS gestionado por Cloudflare (sin Let's Encrypt manual)
- [x] atium.mistborn.cv operativo: /library-agent/actuator/health respondiendo UP
- [x] Arquitectura: una entrada en Cloudflare tunnel por subdominio → todas a NPM → NPM enruta por puerto

#### 3.7 — Backup y monitorización básica
- [x] Backup automático diario gestionado por Proxmox (roshar + scadrial), 3 copias rotativas
- [x] Uptime Kuma configurado: NPM, Portainer, PostgreSQL, MySQL
- [x] Añadir monitor en Kuma: atium.mistborn.cv/library-agent/actuator/health
- [x] Grafana con 3 dashboards: Node Exporter Full, PostgreSQL Database, MySQL Dashboard
- [ ] Spring Boot dashboard en Grafana (Micrometer + Prometheus) — Fase 4

#### 3.8 — Agentes y skills DevOps
- [x] devops-agent: gestiona Dockerfile, docker-compose, variables de entorno y configuración de infraestructura
- [x] ci-agent: especialista en GitHub Actions workflows, GHCR y pipelines de CI/CD
- [x] security-agent: verifica que no hay credenciales en código, gestiona GitHub Secrets, revisa dependencias
- [x] docker-skill: convenciones de Dockerfile multi-stage, docker-compose para este proyecto, health checks
- [x] github-actions-skill: estructura de workflows, secrets de GitHub, patrones de deploy SSH, GHCR

---

### Fase 4 — EN CURSO
Objetivo: autenticación centralizada, motor de recomendaciones y dashboard web.
Todo desarrollado con GitFlow: ramas feature/*, PRs con revisión, CI obligatorio.

#### 4.1 — Authentik: Identity Provider centralizado ✓
Motivación: aprender OAuth2/OIDC de verdad con un IdP estándar reutilizable en todas las apps del homelab.

- [x] Authentik desplegado en roshar via Docker Compose (servicio independiente)
- [x] Tunnel Cloudflare para Authentik: auth.mistborn.cv
- [x] Tenant configurado: application "LibraryAgent", provider OAuth2/OIDC
- [x] Usuario admin creado, grupo `library-admin` definido
- [x] Client ID y Client Secret generados, guardados en GitHub Secrets
- [x] Monitor en Uptime Kuma: auth.mistborn.cv/-/health/live/

#### 4.2 — Spring Security + OIDC
Motivación: aprender el flujo JWT con claims de roles; base para todos los endpoints protegidos.

- [ ] Dependencia `spring-boot-starter-oauth2-resource-server` añadida
- [ ] `SecurityConfig`: STATELESS, validación JWT contra JWKS de Authentik, rutas públicas `/actuator/**`
- [ ] `JwtAuthenticationConverter`: extrae claim `groups` de Authentik → `GrantedAuthority`
- [ ] Roles definidos: `ROLE_ADMIN` (acceso total), `ROLE_VIEWER` (solo lectura)
- [ ] `@PreAuthorize` aplicado en todos los controladores existentes y futuros
- [ ] CORS configurado para el dominio del dashboard (localhost:5173 en dev, dashboard.mistborn.cv en prod)
- [ ] Tests unitarios: `SecurityConfigTest` — verifica rutas públicas y protegidas
- [ ] Tests de integración: `AuthIT` con Testcontainers + token JWT mockeado

#### 4.3 — Perfil lector
Motivación: base de datos sobre los gustos del usuario que alimenta el motor de scoring.

- [ ] Migración Flyway: tabla `user_profile` (géneros favoritos, autores favoritos, idioma preferido, umbral de score mínimo)
- [ ] Migración Flyway: tabla `reading_history` (libro, estado: LEÍDO/EN_CURSO/ABANDONADO/PENDIENTE, fecha, rating 1-5, notas)
- [ ] Entidades JPA + repositorios para ambas tablas
- [ ] `UserProfileService` (interfaz + impl): CRUD de perfil y historial
- [ ] `GET /api/v1/profile` → perfil completo, `PUT /api/v1/profile` → actualizar preferencias
- [ ] `GET /api/v1/reading-history`, `POST /api/v1/reading-history`, `PATCH /api/v1/reading-history/{id}`
- [ ] Tests unitarios del servicio + tests de integración del repositorio (Testcontainers)

#### 4.4 — Motor de recomendaciones
Motivación: núcleo del producto — puntúa libros contra el perfil lector usando Claude.

- [ ] Migración Flyway: tabla `recommendations` (libro, score 0-100, reasoning, estado: NUEVA/VISTA/DESCARTADA, fecha)
- [ ] `BookScoringStrategy` (interfaz sealed): `ClaudeScoringStrategy` y `RuleBasedScoringStrategy` (fallback sin API)
- [ ] `RecommendationService`: cruza libros HIGH/MEDIUM confidence con perfil lector, descarta ya leídos/descartados
- [ ] Prompt Claude Sonnet: recibe perfil + libro, devuelve score + justificación en español
- [ ] Batch configurable: máximo N libros por ejecución (evitar costes excesivos)
- [ ] Caché Redis: scores calculados con TTL de 24h (no recalcular el mismo libro dos veces)
- [ ] `GET /api/v1/recommendations` → lista paginada ordenada por score descendente
- [ ] `PATCH /api/v1/recommendations/{id}/dismiss` → marcar como descartada
- [ ] Tests unitarios: `BookScoringStrategyTest`, `RecommendationServiceTest`
- [ ] Test de integración: `RecommendationControllerIT`

#### 4.5 — Dashboard web
Motivación: interfaz visual para explorar recomendaciones y gestionar el perfil lector.

- [ ] Stack: Next.js 14+ (App Router), TypeScript, Tailwind CSS
- [ ] Autenticación: NextAuth.js con provider OIDC apuntando a Authentik (auth.mistborn.cv)
- [ ] Página principal: lista de recomendaciones con score, portada, título, autor y justificación Claude
- [ ] Página perfil: editar géneros favoritos, autores, umbral de score
- [ ] Página historial: libros leídos/en curso/pendientes con rating y notas
- [ ] Acción "Descartar" en cada recomendación (llama `PATCH /api/v1/recommendations/{id}/dismiss`)
- [ ] Despliegue en roshar: contenedor Docker en docker-compose.prod.yml, subdominio dashboard.mistborn.cv
- [ ] CI ampliado: job de build Next.js + linting TypeScript

---

### Fase 5 — PENDIENTE
Objetivo: automatización completa con n8n, notificaciones Telegram y entrega a Kindle.

#### 5.1 — Workflows n8n
- [ ] n8n desplegado y accesible: n8n.mistborn.cv (ya instalado, falta tunelar y proteger)
- [ ] Workflow "Ingesta diaria": trigger cron 08:00 → webhook Spring Boot `POST /api/v1/ingest/trigger`
- [ ] Workflow "Scoring nocturno": trigger cron 08:30 → webhook Spring Boot `POST /api/v1/recommendations/trigger`
- [ ] Workflow "Resumen semanal": trigger cron lunes 09:00 → genera resumen top-5 recomendaciones → Telegram
- [ ] Webhooks de Spring Boot protegidos con API key interna (no JWT — llamadas máquina a máquina)

#### 5.2 — Bot Telegram
- [ ] Bot registrado en BotFather, token en GitHub Secrets
- [ ] `TelegramNotificationService`: envía mensajes formateados con portada + score + justificación
- [ ] Notificación automática cuando hay recomendaciones nuevas con score > umbral del perfil
- [ ] Comandos básicos: `/top5` (mejores recomendaciones del día), `/perfil` (ver preferencias actuales)
- [ ] Integración con n8n: Telegram como canal de salida de los workflows

#### 5.3 — Entrega a Kindle
- [ ] Skill `kindle-delivery` implementada: descarga epub desde fuente → adjunto email → envío a @kindle.com
- [ ] Workflow n8n "Enviar a Kindle": recibe título desde Telegram → busca epub → envía
- [ ] Gmail configurado como sender (App Password en Secrets)
- [ ] Comando Telegram `/enviar {título}` → desencadena el flujo completo
- [ ] Notificación de confirmación: "📚 _Título_ enviado a tu Kindle"

---

### Fase 6 — PENDIENTE
Objetivo: ampliar fuentes de ingesta y sincronizar biblioteca Kindle.

#### 6.1 — Nuevas fuentes de ingesta
- [ ] `InstagramIngester` via Apify: 3-4 perfiles fijos de bookstagramers, transcripción de Reels
- [ ] `RssIngester`: parser de feeds RSS/Atom para blogs de reseñas (lista configurable de URLs)
- [ ] `GoodreadsIngester`: importación del historial lector del usuario desde export CSV de Goodreads
- [ ] Panel en dashboard para activar/desactivar fuentes y ver estadísticas por fuente

#### 6.2 — Kindle sync
- [ ] Análisis de la API no oficial de Kindle Cloud Reader (autenticación, endpoints de biblioteca)
- [ ] `KindleSyncService`: obtiene lista de libros de la biblioteca Kindle del usuario
- [ ] Sincronización con `reading_history`: libros Kindle → estado EN_CURSO o LEÍDO automáticamente
- [ ] Detección de libros abandonados (comprados pero sin progreso en >30 días) → pregunta por Telegram
- [ ] Sincronización programada: workflow n8n diario

---

### Fase 7 — PENDIENTE
Objetivo: app móvil nativa que consume la API REST existente.

#### 7.1 — App React Native
- [ ] Stack: React Native (Expo), TypeScript
- [ ] Autenticación: OAuth2 PKCE contra Authentik (mismo IdP que el dashboard)
- [ ] Pantalla principal: recomendaciones del día con score y justificación
- [ ] Pantalla detalle libro: portada, metadata completa, botones "Leer después" / "Descartar" / "Enviar a Kindle"
- [ ] Pantalla historial: libros leídos con rating y notas
- [ ] Notificaciones push: integración con el bot Telegram o FCM para nuevas recomendaciones
- [ ] Build y distribución: Expo EAS Build, instalación directa en dispositivo personal (sin App Store)

---

## Lo que Claude Code debe saber siempre
- Este proyecto es para uso personal de Raul, no producción empresarial
- El objetivo secundario es aprender el ecosistema Claude Code completo
- Raul es Senior Java, no necesita explicaciones básicas
- Siempre en español en las respuestas
- Commits en Conventional Commits español
- Nunca hacer commit sin confirmación explícita de Raul
- Fase actual: Fase 4 — En curso. 4.1 completada. Siguiente: 4.2 Spring Security + OIDC
- Todo el desarrollo a partir de ahora via ramas feature/* y PRs
- Nunca push directo a main ni a develop
