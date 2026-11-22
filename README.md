# pasalapeli-bff-service (BFF / puerta de entrada + EKS)

BFF (Backend-for-Frontend) que agrega y protege el acceso a los tres microservicios de dominio (movie, ticket, notification). Despliega sobre **EKS** como Deployment + Service `ClusterIP` (puerto 8082).

## ☁️ Despliegue CI/CD (GitHub Actions → Terraform → EKS)

La infraestructura compartida es propietaria de [`pasalapeli-database`](./../pasalapeli-database) y debe desplegarse primero.

Workflow `.github/workflows/deploy.yml` en cada `push` a `main`:
1. Quality Gate: SonarCloud.
2. Terraform Init / Format / Validate / Plan / Apply (backend S3 `pasalapeli-tfstate`, clave `pasalapeli-bff-service/terraform.tfstate`).
3. `aws eks update-kubeconfig --name pasalapeli-cluster --region us-east-1`
4. `kubectl apply -f k8s-manifests/`

### Kubernetes manifests (`k8s-manifests/`)
- `deployment.yaml`: Deployment `pasalapeli-bff-service` (1 réplica, puerto 8082).
- `service.yaml`: Service `pasalapeli-bff-service` tipo `ClusterIP` (8082 → 8082).

### GitHub Secrets requeridos
| Secret | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Secreto asociado |
| `AWS_SESSION_TOKEN` | Token de sesión (credenciales temporales STS) |
| `SONAR_TOKEN` | Token de SonarCloud |
| `SONAR_HOST_URL` | `https://sonarcloud.io` |

### Variables de entorno (terraform)
- `TF_VAR_environment_name` = `dev`
- `TF_VAR_domain_name` = `pasalapeli-bff-service`

### Imagen ECR
Contenedor extraído de `pasalapeli-images` como `bff-service:latest`.

## ⚙️ Configuración de la aplicación
Véase `src/main/resources/application.yml`. Variables clave: `MOVIE_SERVICE_URL`, `TICKET_SERVICE_URL`, `NOTIFICATION_SERVICE_URL`, `AZURE_*`, `CORS_ALLOWED_ORIGINS`, `KINESIS_STREAM_VENTAS`, `KINESIS_STREAM_SECURITY_LOGS`.
