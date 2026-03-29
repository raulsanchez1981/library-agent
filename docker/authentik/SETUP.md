# Authentik — Pasos de configuración manual

Checklist de tareas manuales tras crear los contenedores. Ejecutar en orden.

---

## 1. Crear la base de datos en scadrial (192.168.1.102)

Conectar a PostgreSQL en scadrial y ejecutar:

```sql
CREATE DATABASE authentik;
-- Reemplazar 'authentik_user' por el valor real de AUTHENTIK_DB_USER en .env.authentik
GRANT ALL ON DATABASE authentik TO authentik_user;
```

---

## 2. Preparar el fichero de entorno en roshar

```bash
# En roshar
cp /ruta/local/.env.authentik.example /opt/libraryagent/.env.authentik
# Editar y rellenar todos los valores reales
nano /opt/libraryagent/.env.authentik
```

Valores a generar:
- `AUTHENTIK_SECRET_KEY`: `openssl rand -base64 60`
- `AUTHENTIK_DB_PASSWORD`: contraseña segura para el usuario de BD
- `AUTHENTIK_BOOTSTRAP_PASSWORD`: contraseña temporal del admin (cambiar tras primer login)

---

## 3. Arrancar los contenedores

```bash
# En roshar, desde el directorio del proyecto
docker compose -f docker-compose.authentik.yml --env-file /opt/libraryagent/.env.authentik up -d

# Verificar estado
docker ps --filter "name=authentik"
docker inspect --format='{{.State.Health.Status}}' authentik-server
docker inspect --format='{{.State.Health.Status}}' authentik-worker
docker inspect --format='{{.State.Health.Status}}' authentik-redis
```

---

## 4. Completar el setup inicial

Desde roshar (acceso local, antes de exponer en NPM):

```
http://localhost:9000/if/flow/initial-setup/
```

Seguir el asistente para establecer la contraseña del administrador (si no se usó `AUTHENTIK_BOOTSTRAP_PASSWORD`).

---

## 5. Configurar Nginx Proxy Manager

Crear proxy host para `auth.mistborn.cv`:
- **Forward Hostname/IP**: `authentik-server`
- **Forward Port**: `9000`
- **Websockets Support**: habilitado (requerido por Authentik)
- En la pestaña Advanced, asegurarse de que el header `Host` se propaga correctamente

Sin el header `Host` correcto, Authentik genera redirect loops.

---

## 6. Exponer en Cloudflare Tunnel

En el dashboard de Cloudflare Tunnel, añadir entrada:
- **Hostname público**: `auth.mistborn.cv`
- **Service**: `http://[IP-NPM]:80` (o el puerto HTTP de NPM en roshar)

---

## 7. Crear Application y Provider en Authentik UI

Acceder a `https://auth.mistborn.cv` y completar:

1. **Applications > Providers > Create**
   - Tipo: OAuth2/OpenID Connect Provider
   - Nombre: `LibraryAgent`
   - Client type: `Confidential`
   - Redirect URIs: `https://libraryagent.mistborn.cv/library-agent/login/oauth2/code/authentik`
   - Scopes: `openid`, `profile`, `email`

2. **Applications > Applications > Create**
   - Nombre: `LibraryAgent`
   - Slug: `libraryagent`
   - Provider: el creado en el paso anterior

3. **Directory > Groups > Create**
   - Nombre: `library-admin`
   - Asignar el usuario administrador al grupo

---

## 8. Guardar credenciales OIDC

Tras crear el Provider, anotar:
- **Client ID**
- **Client Secret**

Guardar en GitHub Secrets del repositorio:
- `AUTHENTIK_CLIENT_ID`
- `AUTHENTIK_CLIENT_SECRET`

Añadir también al fichero `/opt/libraryagent/.env.prod` en roshar:

```
AUTHENTIK_CLIENT_ID=<valor-real>
AUTHENTIK_CLIENT_SECRET=<valor-real>
AUTHENTIK_ISSUER_URL=https://auth.mistborn.cv/application/o/libraryagent/
```

---

## 9. Añadir monitor en Uptime Kuma

- **Tipo**: HTTP(s)
- **URL**: `https://auth.mistborn.cv/healthz`
- **Intervalo**: 60 segundos
- **Nombre**: `Authentik`
