# Authentik — Pasos de configuración manual

Checklist de tareas manuales tras crear los contenedores. Ejecutar en orden.

---

## 1. Crear usuario y base de datos en scadrial (192.168.1.102)

```bash
# Desde tu máquina de desarrollo, conectar a scadrial
ssh raul@192.168.1.102

# Abrir consola PostgreSQL como superusuario
sudo -u postgres psql
```

Dentro de `psql`, ejecutar:

```sql
CREATE USER authentik WITH PASSWORD 'elige-una-password-segura';
CREATE DATABASE authentik OWNER authentik;
GRANT ALL PRIVILEGES ON DATABASE authentik TO authentik;
\q
```

Apunta la password elegida: la necesitarás en el paso 2 como `AUTHENTIK_DB_PASSWORD`.

---

## 2. Preparar el fichero de entorno en roshar

```bash
# Desde tu máquina de desarrollo, conectar a roshar
ssh raul@roshar   # o la IP que corresponda

# Ir al directorio del proyecto
cd /opt/libraryagent

# Actualizar el repo
git pull origin main

# Generar la secret key (copia el resultado)
openssl rand -base64 60

# Crear el fichero de entorno a partir de la plantilla
cp .env.authentik.example .env.authentik

# Editar y rellenar los valores
nano .env.authentik
```

Valores a completar en `.env.authentik`:

| Variable | Valor |
|---|---|
| `AUTHENTIK_DB_NAME` | `authentik` |
| `AUTHENTIK_DB_USER` | `authentik` |
| `AUTHENTIK_DB_PASSWORD` | la password del paso 1 |
| `AUTHENTIK_SECRET_KEY` | resultado del `openssl rand -base64 60` |
| `AUTHENTIK_BOOTSTRAP_EMAIL` | tu email de admin |
| `AUTHENTIK_BOOTSTRAP_PASSWORD` | contraseña temporal (la cambiarás tras el primer login) |

---

## 3. Arrancar los contenedores

```bash
# En roshar, desde /opt/libraryagent
docker compose -f docker-compose.authentik.yml --env-file .env.authentik up -d
```

Verificar que los tres contenedores arrancan correctamente (tarda ~60 segundos):

```bash
# Ver estado general
docker ps --filter "name=authentik"

# Esperar a que los health checks pasen
watch -n5 'docker inspect --format="{{.Name}}: {{.State.Health.Status}}" authentik-server authentik-worker authentik-redis'
# Ctrl+C cuando los tres muestren "healthy"
```

Si alguno falla, revisar logs:

```bash
docker logs authentik-server --tail 50
docker logs authentik-worker --tail 50
```

---

## 4. Setup inicial desde la UI

Con los contenedores healthy, abrir desde roshar (acceso local antes de exponerlo):

```
http://localhost:9000/if/flow/initial-setup/
```

> Si accedes desde tu máquina de desarrollo puedes hacer un túnel SSH temporal:
> ```bash
> ssh -L 9000:localhost:9000 raul@roshar
> ```
> y luego abrir `http://localhost:9000/if/flow/initial-setup/` en tu navegador.

Seguir el asistente: establecerá la contraseña del administrador usando `AUTHENTIK_BOOTSTRAP_EMAIL` y `AUTHENTIK_BOOTSTRAP_PASSWORD`.

---

## 5. Configurar Nginx Proxy Manager

En la UI de NPM, crear un nuevo **Proxy Host**:

- **Domain Names**: `auth.mistborn.cv`
- **Scheme**: `http`
- **Forward Hostname/IP**: IP de roshar en red local (ej. `192.168.1.101`)
- **Forward Port**: `9000`
- **Websockets Support**: **habilitado** (obligatorio para Authentik)
- **Block Common Exploits**: habilitado

En la pestaña **SSL**: solicitar certificado Let's Encrypt o usar el de Cloudflare.

> Sin el header `Host` correcto Authentik genera redirect loops. NPM lo propaga automáticamente.

---

## 6. Exponer en Cloudflare Tunnel

En el dashboard de Cloudflare Tunnel, añadir una nueva entrada pública:

- **Subdomain**: `auth`
- **Domain**: `mistborn.cv`
- **Type**: `HTTP`
- **URL**: `[IP-roshar]:80` (puerto HTTP de NPM)

Verificar que `https://auth.mistborn.cv` carga el login de Authentik.

---

## 7. Crear Application y Provider en Authentik UI

Acceder a `https://auth.mistborn.cv` con el usuario administrador.

### 7.1 Crear el Provider

**Applications > Providers > Create**:

- **Type**: OAuth2/OpenID Connect
- **Name**: `LibraryAgent`
- **Client type**: `Confidential`
- **Redirect URIs**: `https://libraryagent.mistborn.cv/library-agent/login/oauth2/code/authentik`
- **Scopes**: `openid`, `profile`, `email`
- El resto de campos dejar por defecto

### 7.2 Crear la Application

**Applications > Applications > Create**:

- **Name**: `LibraryAgent`
- **Slug**: `libraryagent`
- **Provider**: seleccionar `LibraryAgent` (el creado en 7.1)

### 7.3 Crear el grupo de administradores

**Directory > Groups > Create**:

- **Name**: `library-admin`

**Directory > Users** → entrar en tu usuario administrador → pestaña **Groups** → añadir `library-admin`.

---

## 8. Guardar credenciales OIDC

En Authentik, ir a **Applications > Providers > LibraryAgent** y anotar:
- **Client ID**
- **Client Secret** (botón "Copy" o "Regenerate")

### En GitHub Secrets

En `https://github.com/raulsanchez1981/library-agent/settings/secrets/actions`, añadir:

| Secret | Valor |
|---|---|
| `AUTHENTIK_CLIENT_ID` | el Client ID copiado |
| `AUTHENTIK_CLIENT_SECRET` | el Client Secret copiado |

### En roshar

```bash
# En roshar, editar el .env.prod
nano /opt/libraryagent/.env.prod
```

Añadir al final:

```
AUTHENTIK_CLIENT_ID=<valor-real>
AUTHENTIK_CLIENT_SECRET=<valor-real>
AUTHENTIK_ISSUER_URL=https://auth.mistborn.cv/application/o/libraryagent/
```

---

## 9. Añadir monitor en Uptime Kuma

En la UI de Uptime Kuma, crear nuevo monitor:

- **Monitor Type**: HTTP(s)
- **Friendly Name**: `Authentik`
- **URL**: `https://auth.mistborn.cv/healthz`
- **Heartbeat Interval**: 60 segundos

---

## Checklist final

- [ ] Base de datos `authentik` creada en scadrial con usuario dedicado
- [ ] `.env.authentik` rellenado en roshar
- [ ] Los tres contenedores en estado `healthy`
- [ ] Setup inicial completado en `http://localhost:9000/if/flow/initial-setup/`
- [ ] `auth.mistborn.cv` accesible via NPM + Cloudflare Tunnel
- [ ] Provider OAuth2/OIDC y Application `LibraryAgent` creados en Authentik
- [ ] Grupo `library-admin` creado y usuario admin asignado
- [ ] `AUTHENTIK_CLIENT_ID` y `AUTHENTIK_CLIENT_SECRET` en GitHub Secrets
- [ ] Variables OIDC añadidas a `.env.prod` en roshar
- [ ] Monitor `auth.mistborn.cv/healthz` en Uptime Kuma
