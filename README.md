# Acento — API de franquicias

Prueba técnica: administración de **franquicias → sucursales → productos** con API reactiva,
autenticación por JWT en cookie y frontend SPA. Este README es la guía del proyecto: qué es,
cómo está armado, cómo correrlo en local y cómo publicarlo en producción.

## Composición

| Capa | Tecnología |
|---|---|
| Backend | Java 25 · Spring Boot 4.1 · WebFlux · Spring Data R2DBC · Flyway |
| Base de datos | PostgreSQL 18 |
| Frontend | Angular 22 (standalone, zoneless) |
| Borde local | nginx 1.29 con TLS de `prueba-accenture.com` (mkcert) |
| Desarrollo | Docker Compose · Bun como task runner |
| Producción | Netlify (frontend) · Render (backend) · Neon (PostgreSQL) |

## Cómo funciona

**Capas del backend.** Cada request entra por un `@RestController` (uno por prefijo de ruta),
que delega en un `@Service`; los servicios usan repositorios de Spring Data R2DBC: consultas
derivadas, entidades con `@Version` para bloqueo optimista y paginación, búsqueda y estadísticas
resueltas en memoria sobre las filas leídas (el volumen es de demo).

**Modelo de datos.** `franchises` → `branches` → `products` con borrado en cascada por claves
foráneas. `users`, `roles` y `permissions` se relacionan por las tablas puente `user_roles` y
`role_permissions`; `user_franchises` define el alcance de cada `USER`. Roles y permisos nacen
en las migraciones de Flyway (no hay CRUD de roles).

**Seguridad.** Login con BCrypt que emite un JWT HS256 en la cookie `access_token`
(`HttpOnly`, `Secure`, `SameSite=Lax`). CSRF con doble cookie: `XSRF-TOKEN` legible por JS y
header `X-XSRF-TOKEN` obligatorio en métodos inseguros. La autorización combina reglas por
ruta en la cadena de seguridad y `@PreAuthorize` por permiso (`READ`, `CREATE`, `UPDATE`,
`DELETE`, `UPDATE_OWN`). Cualquier ruta no declarada se rechaza.

**Frontend.** SPA Angular con tres vistas de negocio (`/franchises`, `/branches`, `/users`),
login y registro públicos, guards de sesión y de rol, e interceptor que agrega el header CSRF.

## Estructura

```
backend/                     Spring Boot
  src/main/java/com/accenture/franchises/
    controllers/             un controlador por prefijo de ruta
    services/                reglas de negocio
    repositories/            Spring Data R2DBC
    entities/                tablas
    contracts/request/       cuerpos de entrada
    contracts/response/      cuerpos de salida
    security/                JWT, cookie, CSRF, throttle de login
    config/                  seguridad, caché, propiedades
    exceptions/              ApiException + handler RFC 7807
  src/main/resources/db/migration/   migraciones Flyway
  Dockerfile                 imagen del backend
frontend/                    Angular 22 standalone
  src/app/views/             páginas (franchises, branches, branch-detail, users, login, register)
  src/app/components/        UI reutilizable (button, input, table, modal, menu, layout)
  src/app/services/          acceso a la API
  src/app/guards/            authGuard, adminGuard, guestGuard
  Dockerfile                 dev server para Compose
infra/
  docker/compose.yml         postgres + backend + frontend + nginx (desarrollo)
  docker/nginx/              configuración del borde local
  docker/certs/              certificado local (no versionado)
  terraform/                 web service del backend en Render
docs/postman/                colección de la API
scripts/                     cert.sh, hosts.ps1
.env                         única fuente de credenciales (no versionado)
```

## Local

Requisitos: Docker con motor Linux, Bun 1.3+ y mkcert (solo la primera vez).

1. Crear el `.env` en la raíz. Variables:

```
POSTGRES_DB=franchises
POSTGRES_USER=franchises
DATABASE_PASSWORD=<clave de la base>
SPRING_PROFILES_ACTIVE=dev
FLYWAY_ENABLED=true
JWT_SECRET=<32 caracteres o más>
TOKEN_TTL=PT30M
COOKIE_SECURE=true
ADMIN_EMAIL=<correo del admin>
ADMIN_PASSWORD=<clave: 8+, un número y un símbolo>
CORS_ALLOWED_ORIGINS=https://prueba-accenture.com,http://localhost:4200
USER_PASSWORD=<clave de los usuarios de prueba>
```

2. Certificado y host: `bun run cert` y `powershell -ExecutionPolicy Bypass -File scripts\hosts.ps1`
   (este último en consola elevada, apunta `prueba-accenture.com` a 127.0.0.1).
3. `bun run up` construye y levanta los cuatro contenedores. La app queda en
   <https://prueba-accenture.com> y la API en <https://prueba-accenture.com/api/v1>.
4. El administrador se crea solo, una vez, con `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

| Comando | Qué hace |
|---|---|
| `bun run up` / `bun run down` | levanta / apaga el stack |
| `bun run logs` / `bun run ps` | logs en vivo / estado |
| `bun run rebuild:backend` / `rebuild:frontend` | reconstruye un servicio |
| `bun run db:shell` / `bun run db:reset` | psql del contenedor / borra el volumen y recrea |
| `bun run config` | valida el compose resuelto |

Las migraciones ya aplicadas no se editan: si se cambia una, Flyway falla por checksum y hay
que correr `flyway repair` contra la base (el `db:reset` recrea el volumen desde cero).

## Producción

| Parte | Servicio | URL / configuración |
|---|---|---|
| Frontend | Netlify | https://acento-prueba.netlify.app · build y publicación desde `netlify.toml` |
| Backend | Render (plan free, Docker) | https://acento-api.onrender.com · servidor desde `backend/Dockerfile`, 512 MB, duerme a los 15 min sin tráfico |
| Base de datos | Neon (plan free) | proyecto `acento` en `aws-us-west-2`, PostgreSQL 18 |

**Backend con Terraform.**

```bash
cd infra/terraform
export RENDER_API_KEY=<api key de Render>
terraform init
terraform apply \
  -var repo_url=https://github.com/<usuario>/<repo> \
  -var frontend_origin=https://<sitio>.netlify.app \
  -var database_url='r2dbc:postgresql://<host>/<db>?sslMode=require' \
  -var jdbc_url='jdbc:postgresql://<host>/<db>?sslmode=require' \
  -var database_user=<usuario> -var database_password=<clave> \
  -var jwt_secret=<32+ caracteres> \
  -var admin_email=<correo> -var admin_password=<clave>
terraform output service_url
```

El apply crea el web service, carga todas las variables de entorno, define el healthcheck en
`/actuator/health` y deja el deploy automático en cada push. Flyway aplica las migraciones al
arrancar contra Neon.

Variables del web service: `SPRING_PROFILES_ACTIVE`, `DATABASE_URL`, `JDBC_URL`, `DATABASE_USER`,
`DATABASE_PASSWORD`, `JWT_SECRET`, `TOKEN_TTL`, `COOKIE_SECURE`, `COOKIE_SAME_SITE`,
`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `FLYWAY_ENABLED`, `JAVA_TOOL_OPTIONS`.

## API

Base: `/api/v1`. Permiso = autoridad exigida por `@PreAuthorize`; el alcance de cada `USER` se
valida además contra `user_franchises` (403 si la franquicia no es suya).

| # | Método | Ruta | Permiso | Controlador |
|---|---|---|---|---|
| 1 | POST | `/auth/login` | público | `AuthController` |
| 2 | POST | `/auth/register` | público | `AuthController` |
| 3 | POST | `/auth/logout` | sesión | `AuthController` |
| 4 | GET | `/auth/me` | sesión | `AuthController` |
| 5 | PATCH | `/auth/me` | `UPDATE_OWN` | `AuthController` |
| 6 | GET | `/auth/csrf` | público | `AuthController` |
| 7 | GET | `/users?page&size` | ADMIN | `UserController` |
| 8 | GET | `/users/{id}` | ADMIN | `UserController` |
| 9 | POST | `/users` | ADMIN | `UserController` |
| 10 | PATCH | `/users/{id}` | ADMIN | `UserController` |
| 11 | DELETE | `/users/{id}` | ADMIN | `UserController` |
| 12 | GET | `/franchises?page&size` | `READ` | `FranchiseController` |
| 13 | GET | `/franchises/{fid}` | `READ` | `FranchiseController` |
| 14 | POST | `/franchises` | `CREATE` | `FranchiseController` |
| 15 | PATCH | `/franchises/{fid}?version` | `UPDATE` | `FranchiseController` |
| 16 | DELETE | `/franchises/{fid}` | `DELETE` | `FranchiseController` |
| 17 | PUT | `/franchises/{fid}/members` | `UPDATE` | `FranchiseController` |
| 18 | GET | `/franchises/{fid}/branches` | `READ` | `BranchController` |
| 19 | POST | `/franchises/{fid}/branches` | `CREATE` | `BranchController` |
| 20 | PATCH | `/franchises/{fid}/branches/{bid}` | `UPDATE` | `BranchController` |
| 21 | DELETE | `/franchises/{fid}/branches/{bid}` | `DELETE` | `BranchController` |
| 22 | GET | `/franchises/{fid}/products?page&size&search` | `READ` | `ProductController` |
| 23 | POST | `/franchises/{fid}/branches/{bid}/products` | `CREATE` | `ProductController` |
| 24 | PATCH | `/franchises/{fid}/branches/{bid}/products/{pid}` | `UPDATE` | `ProductController` |
| 25 | PATCH | `/franchises/{fid}/branches/{bid}/products/{pid}/stock` | `UPDATE` | `ProductController` |
| 26 | DELETE | `/franchises/{fid}/branches/{bid}/products/{pid}` | `DELETE` | `ProductController` |
| 27 | GET | `/actuator/health` | público | Actuator |

Convenciones: `201` + `Location` al crear, `204` al borrar, `409` si la `version` enviada no es
la actual, `404` cuando el recurso padre no existe y `403` fuera de alcance.

## Pruebas y colección

```bash
cd backend && ./gradlew test
```

27 tests de integración con Testcontainers (PostgreSQL 18 real): auth, CSRF, usuarios, roles,
alcance por franquicia, CRUD de franquicias/sucursales/productos, concurrencia y validaciones.
Las credenciales salen del `.env` de la raíz.

La colección de Postman está en `docs/postman/Acento-API.postman_collection.json` (27 requests
en orden, con tests por request). Las variables `adminEmail`, `adminPassword` y `userPassword`
se completan con los valores del `.env`.
