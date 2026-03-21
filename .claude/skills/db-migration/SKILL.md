# Skill: Migraciones de base de datos con Flyway

## Nomenclatura

```
V{version}__{descripcion}.sql
```

- `{version}`: número incremental con dos dígitos mínimo, ej: `V01`, `V02`, `V15`
- `{descripcion}`: palabras separadas por guión bajo, en inglés, ej: `create_books_table`
- Separador: **doble guión bajo** `__`

Ejemplos válidos:
```
V01__create_users_table.sql
V02__create_books_table.sql
V03__add_score_column_to_books.sql
V14__create_recommendation_history_table.sql
```

## Ubicación

```
src/main/resources/db/migration/
```

Flyway escanea este directorio automáticamente al arrancar la aplicación.

## Regla fundamental

**Nunca modificar una migración ya aplicada.** Si hay un error, crear una nueva migración que lo corrija.

## Estructura de una migración típica

```sql
-- V05__add_kindle_email_to_users.sql

ALTER TABLE users
    ADD COLUMN kindle_email VARCHAR(255);

COMMENT ON COLUMN users.kindle_email IS 'Dirección @kindle.com para entrega de libros';
```

## Convenciones

- Siempre incluir `IF NOT EXISTS` / `IF EXISTS` donde aplique para idempotencia
- Las migraciones de datos (no de esquema) van en archivos separados con prefijo `R__` (repetibles) solo si son verdaderamente idempotentes
- Añadir comentario en la primera línea indicando qué hace la migración
- No mezclar DDL y DML en la misma migración salvo que sean inseparables

## Verificación

Para comprobar el estado de migraciones aplicadas:
```bash
mvn flyway:info
```

Para reparar checksum tras un fix de emergencia en desarrollo (nunca en producción):
```bash
mvn flyway:repair
```
