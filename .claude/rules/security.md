# Reglas: Seguridad

## Credenciales y secretos

- **Nunca** hardcodear credenciales, tokens, API keys o contraseñas en código fuente
- Siempre usar variables de entorno referenciadas en `application.yml`:
  ```yaml
  claude:
    api-key: ${CLAUDE_API_KEY}
  ```
- El fichero `.env` nunca se commitea; está en `.gitignore`
- En tests, usar valores de placeholder (`test-api-key`, `dummy-secret`) — nunca credenciales reales

## Logging

- **Nunca** loguear datos sensibles: contraseñas, tokens, API keys, emails completos, datos personales
- Enmascarar identificadores personales en logs:
  ```java
  log.info("Entregando libro a ***@kindle.com");          // correcto
  log.info("Entregando libro a {}", kindleEmail);         // incorrecto
  ```
- Nivel `DEBUG` puede ser más verboso, pero igualmente sin secretos

## Variables de entorno requeridas

Documentar todas las variables de entorno en `README.md` o `docker-compose.yml` con valores de ejemplo (no reales):

```yaml
environment:
  - CLAUDE_API_KEY=your-api-key-here
  - KINDLE_SENDER_EMAIL=your-email@gmail.com
  - DB_PASSWORD=change-me
```

## Dependencias

- Mantener dependencias actualizadas; revisar vulnerabilidades con:
  ```bash
  mvn dependency:check
  ```
