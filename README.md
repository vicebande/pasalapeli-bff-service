# 🛡️ Pasa La Peli - Backend-For-Frontend (BFF Service)

Microservicio orquestador de API y seguridad que actúa como **punto de entrada real de la API** (detrás del proxy `/api` de Nginx; no existe AWS API Gateway), encargado de la validación criptográfica de tokens JWT de **Microsoft Azure AD (MSAL)** y enrutamiento hacia los microservicios de dominio.

---

## 🚀 Tecnologías
- **Java 21**
- **Spring Boot 3.3.3**
- **Spring Security 6.x**
- **Spring OAuth2 Resource Server (Nimbus JWT)**
- **RestTemplate / Reverse-Proxy**
- **Docker & Dockerfile**

---

## 🔐 Validación JWT
El BFF implementa todas las validaciones de token:
1. **Firma criptográfica:** Valida mediante el conjunto de llaves públicas JWKS de Azure AD (`jwk-set-uri`).
2. **Emisor (Issuer):** Valida que el claim `iss` corresponda al tenant de Azure configurado.
3. **Audiencia (Audience):** Mediante la clase `AudienceValidator`, verifica que el claim `aud` coincida con el `clientId` o `app-id-uri`.
4. **Vigencia (Expiration):** Valida marcas temporales (`exp`, `nbf`, `iat`) con `JwtTimestampValidator`.
5. **Autorización basada en roles:** Extrae roles desde claims (`roles`, `scp`, `groups`) con `JwtAuthConverter` para autorizar rutas administrativas (`ROLE_ADMIN`) o de cliente (`ROLE_CLIENTE`).
6. **Modo Desarrollo:** Incluye `DevMockAuthFilter` para emular roles en desarrollo local sin requerir suscripción activa en Azure.

---

## ⚙️ Configuración (`application.yml`)
- **Puerto:** `8080`
- **Azure AD:**
  - `azure.activedirectory.enabled`: `true` / `false`
  - `azure.activedirectory.client-id`: Application (client) ID de Azure
  - `azure.activedirectory.tenant-id`: Directory (tenant) ID de Azure
- **Microservicios Conectados:**
  - `services.movie-service.url`: `http://localhost:8082`
  - `services.ticket-service.url`: `http://localhost:8083`
  - `services.notification-service.url`: `http://localhost:8084`

---

## 📡 Endpoints Expuestos
- `GET /api/cartelera`: Catálogo de cartelera para el cliente (público).
- `GET /api/cartelera/{id}`: Detalle de película y funciones.
- `POST /api/tickets/comprar`: Compra de tickets (autenticado).
- `GET /api/tickets/usuario/{usuarioId}`: Historial de compras (autenticado).
- `GET /api/auth/me`: Retorna perfil y roles del usuario autenticado.
- `GET /api/notificaciones`: Bandeja de notificaciones del usuario autenticado.
- `GET /api/notificaciones/no-leidas/count`: Contador de notificaciones no leídas.
- `POST /api/notificaciones/{id}/leer`: Marca una notificación como leída.
- `POST /api/notificaciones/leer-todas`: Marca todas las notificaciones como leídas.
- `POST /api/admin/movies`: Creación de películas y subida de portadas al almacenamiento local (requiere `ROLE_ADMIN`).
- `DELETE /api/admin/movies/{id}`: Eliminación de película (requiere `ROLE_ADMIN`).
- `POST /api/admin/funciones`: Programación de funciones (requiere `ROLE_ADMIN`).
- `GET|POST|DELETE /api/admin/{destinos,reglas,mensajes-fallidos}`: Administración de la mensajería (requiere `ROLE_ADMIN`).

---

## 🛠️ Ejecución Local
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```
O con Docker:
```bash
docker build -t bff-service .
docker run -p 8080:8080 bff-service
```

---

## ☁️ Despliegue CI/CD (GitHub Actions → EC2)

Este repo se despliega **solo a sí mismo** sobre una instancia **EC2 (Ubuntu 24.04)** que ya porta el stack completo. El orquestador vive en el repo [`pasalapeli-database`](https://github.com) (contiene el `docker-compose.yml` global en `/opt/pasalapeli/`).

### Workflow `.github/workflows/deploy.yml`
En cada `push` a `main`:
1. SSH al EC2 (acción `appleboy/ssh-action`).
2. `git pull` del código de `bff-service` en `/opt/pasalapeli/pasalapeli-bff-service`.
3. `docker compose up -d --build bff-service`.
4. Espera el estado `healthy` del contenedor vía `/actuator/health`.

### GitHub Secrets requeridos en este repo
| Secret | Descripción |
|---|---|
| `EC2_HOST` | IP pública del EC2 |
| `EC2_USER` | Usuario SSH (usualmente `ubuntu`) |
| `EC2_SSH_KEY` | Clave privada SSH (.pem) |

### Variables de entorno en producción (definidas en el `.env` del orquestador)
- `AZURE_AUTH_ENABLED=true`
- `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_APP_ID_URI`
- `AZURE_AD_ISSUER_URI=https://login.microsoftonline.com/<tenant-id>/v2.0`
- `AZURE_AD_JWK_SET_URI=https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys`
- `MOVIE_SERVICE_URL=http://movie-service:8082`
- `TICKET_SERVICE_URL=http://ticket-service:8083`
- `NOTIFICATION_SERVICE_URL=http://notification-service:8084`
- `CORS_ALLOWED_ORIGINS=https://<tu-dominio>`
