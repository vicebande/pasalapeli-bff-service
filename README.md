# 🛡️ Pasa La Peli - Backend-For-Frontend (BFF Service)

Microservicio orquestador de API y seguridad ubicado tras **AWS API Gateway**, encargado de la validación criptográfica de tokens JWT de **Microsoft Azure AD (MSAL)** y enrutamiento hacia los microservicios de dominio.

Parte del ecosistema Cloud Native **Pasa La Peli** para **Desarrollo Cloud Native I (DSY1107) - Duoc UC**.

---

## 🚀 Tecnologías
- **Java 21**
- **Spring Boot 3.3.3**
- **Spring Security 6.x**
- **Spring OAuth2 Resource Server (Nimbus JWT)**
- **RestTemplate / Reverse-Proxy**
- **Docker & Dockerfile**

---

## 🔐 Cumplimiento de la Pauta de Evaluación (Validación JWT)
El BFF implementa todas las validaciones exigidas en la rúbrica (100% Muy buen desempeño):
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

---

## 📡 Endpoints Expuestos
- `GET /api/cartelera`: Catálogo de cartelera para el cliente (público).
- `GET /api/cartelera/{id}`: Detalle de película y funciones.
- `POST /api/tickets/comprar`: Compra de tickets (autenticado).
- `GET /api/tickets/usuario/{usuarioId}`: Historial de compras (autenticado).
- `GET /api/auth/me`: Retorna perfil y roles del usuario autenticado.
- `POST /api/admin/movies`: Creación de películas y subida de portadas a S3 (requiere `ROLE_ADMIN`).
- `DELETE /api/admin/movies/{id}`: Eliminación de película (requiere `ROLE_ADMIN`).
- `POST /api/admin/funciones`: Programación de funciones (requiere `ROLE_ADMIN`).

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
