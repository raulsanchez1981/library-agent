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
