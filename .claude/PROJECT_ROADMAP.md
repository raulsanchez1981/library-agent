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

#### 4.2 — Spring Security + OIDC ✓
Motivación: aprender el flujo JWT con claims de roles; base para todos los endpoints protegidos.

- [x] Dependencia `spring-boot-starter-oauth2-resource-server` añadida
- [x] `SecurityConfig`: STATELESS, validación JWT contra JWKS de Authentik, rutas públicas `/actuator/**`
- [x] `LibraryAgentJwtConverter`: extrae claim `groups` de Authentik → `ROLE_ADMIN` / `ROLE_VIEWER`
- [x] Roles definidos: `ROLE_ADMIN` (acceso total), `ROLE_VIEWER` (solo lectura)
- [x] `@EnableMethodSecurity` activo — `@PreAuthorize` listo para futuros controladores
- [x] CORS configurable vía `app.cors.allowed-origins` (localhost:5173 dev, dashboard.mistborn.cv prod)
- [x] `SecurityConfigTest` (5 tests) + `LibraryAgentJwtConverterTest` (4 tests) — 53/53 en verde
- [x] `GlobalExceptionHandler` corregido: re-throw de `AccessDeniedException` y `NoResourceFoundException`
- [x] Authentik UI: scope mapping `groups` configurado en el provider LibraryAgent ⚠️ verificación end-to-end pendiente hasta Fase 4.5 (dashboard) o Fase 7 (app móvil)

#### 4.3 — Perfil lector ✓
Motivación: base de datos sobre los gustos del usuario que alimenta el motor de scoring.

- [x] Migración Flyway V7: campos en `user_profile` (géneros, autores, idioma, umbral)
- [x] Migración Flyway V8: tabla `reading_history` (estado, fechas, rating, notas)
- [x] Entidades JPA + repositorios: `UserProfile` ampliado, `ReadingHistoryEntity`, `ReadingHistoryRepository`
- [x] `UserProfileService` (interfaz + `UserProfileServiceImpl`): getOrCreate por email, updatePreferences
- [x] `ReadingHistoryService` (interfaz + `ReadingHistoryServiceImpl`): findAll, add, update
- [x] `GET /api/v1/profile`, `PUT /api/v1/profile` — usuario resuelto desde JWT claim `email`
- [x] `GET /api/v1/reading-history`, `POST /api/v1/reading-history`, `PATCH /api/v1/reading-history/{id}`
- [x] 12 tests unitarios (UserProfileServiceTest x6, ReadingHistoryServiceTest x6) — 65/65 en verde

#### 4.4 — Motor de recomendaciones ✓

- [x] Migración Flyway V9: tabla `recommendations` (score 0-100, reasoning, estado: NUEVA/VISTA/DESCARTADA)
- [x] `BookScoringStrategy` (interfaz): `ClaudeScoringStrategy` (Sonnet) y `RuleBasedScoringStrategy` (fallback sin API)
- [x] `RecommendationService`: cruza libros HIGH/MEDIUM confidence con perfil lector, descarta ya leídos
- [x] Prompt Claude Sonnet externalizado en `resources/prompts/recommendation.txt`
- [x] Batch configurable: `POST /api/v1/recommendations/trigger?maxBatch=20`
- [x] Caché Redis: scores calculados con TTL de 24h
- [x] `GET /api/v1/recommendations` → lista paginada ordenada por score descendente
- [x] `PATCH /api/v1/recommendations/{id}/dismiss` → marcar como descartada
- [x] 9 tests unitarios (BookScoringStrategyTest x4, RecommendationServiceTest x5) — 68/68 en verde

#### 4.5 — Dashboard web
Motivación: interfaz visual para explorar recomendaciones y gestionar el perfil lector.
Stack: Next.js 14+ (App Router), TypeScript, Tailwind CSS. Dentro del monorepo en `dashboard/`, separable en el futuro.

##### 4.5.1 — Scaffolding y layout base ✓
- [x] `dashboard/` en la raíz del monorepo: Next.js 16.2.2 con TypeScript, Tailwind, App Router
- [x] Layout principal: sidebar oscuro con navegación (Recomendaciones / Perfil / Historial)
- [x] Route group `(dashboard)` con páginas placeholder para las 3 secciones
- [x] Variables de entorno documentadas en `.env.local.example`

##### 4.5.2 — Autenticación con Authentik ✓
- [x] NextAuth.js v5 con provider Authentik OIDC (`auth.mistborn.cv`)
- [x] `proxy.ts` (convención Next.js 16) protege todas las rutas, redirige a Authentik si no hay sesión
- [x] Sidebar muestra nombre/email del usuario autenticado y botón de logout
- [x] `access_token` de Authentik guardado en sesión para propagar a la API Spring Boot

##### 4.5.3 — Página de recomendaciones ✓
- [x] `GET /api/v1/recommendations` → lista paginada con score, título, autor, justificación Claude
- [x] Badge de score con color por rango (verde ≥80, amarillo 60-79, rojo <60)
- [x] Botón "Descartar" con UI optimista → `PATCH /api/v1/recommendations/{id}/dismiss`
- [x] Botón "Lanzar scoring" con feedback → `POST /api/v1/recommendations/trigger`
- [x] Paginación y skeleton de carga

##### 4.5.4 — Páginas de perfil e historial ✓
- [x] Perfil: formulario para editar géneros favoritos, autores y umbral mínimo de score (`PUT /api/v1/profile`)
- [x] Historial: lista de libros con estado, rating y notas (`GET /api/v1/reading-history`)
- [x] Añadir libro al historial con formulario inline (`POST /api/v1/reading-history`)
- [x] Cambiar estado de un libro (`PATCH /api/v1/reading-history/{id}`)

##### 4.5.5 — Dockerización y despliegue
- [ ] `dashboard/Dockerfile` multi-stage (build Node + runtime distroless/node)
- [ ] Servicio `dashboard` en `docker-compose.prod.yml` (puerto 3000)
- [ ] Subdominio `dashboard.mistborn.cv` en Cloudflare Tunnel → NPM → dashboard:3000
- [ ] CI ampliado: job `build-dashboard` con `npm ci`, `npm run lint`, `npm run build`
- [ ] Monitor en Uptime Kuma: `dashboard.mistborn.cv`

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
- Fase actual: Fase 4 — En curso. 4.1–4.4 y 4.5.1–4.5.4 completadas. Siguiente: 4.5.5 Dockerización y despliegue del dashboard
- Todo el desarrollo a partir de ahora via ramas feature/* y PRs
- Nunca push directo a main ni a develop
