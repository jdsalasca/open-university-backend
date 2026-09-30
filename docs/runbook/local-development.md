# Runbook de desarrollo local

## Requisitos

- Docker Desktop con Docker Compose v2.23 o posterior y motor en ejecución.
- Git y GitHub CLI autenticado con acceso a los dos repositorios privados.
- Para compilar Java directamente en el host: SDKMAN con la versión declarada en `.sdkmanrc` (`25.0.4-tem`). El flujo Docker usa una imagen Maven basada en JDK 25.
- Para pruebas del frontend en el host: Node.js 24 y npm.

## Preparar el checkout

```powershell
gh repo clone jdsalasca/Universiry-backend
Set-Location Universiry-backend
git switch develop
git submodule update --init --recursive
```

El checkout integra `Universiry-frontend` en `frontend/`. Después de actualizar el branch del submódulo, integrar y confirmar el nuevo gitlink en el backend.

## Iniciar y observar

```powershell
docker compose config --quiet
docker compose up --build -d --wait
```

En una segunda terminal:

```powershell
docker compose watch --no-up
```

`compose watch` permanece activo en esa terminal. Los cambios bajo `frontend/src` se sincronizan a Vite y activan HMR; cambios de manifiestos reconstruyen la imagen. Los cambios bajo `backend/src` reinician el proceso Maven/Spring; cambios al `pom.xml` reconstruyen el contenedor.

Endpoints locales: UI `http://localhost:5173`, API `http://localhost:8080`, salud `http://localhost:8080/actuator/health`. El proxy de Vite dirige `/api` y `/assets` al servicio backend dentro de la red privada de Compose.

## Variables locales

Compose ofrece credenciales sencillas solo para desarrollo. Para cambiarlas, copia `.env.example` a `.env`, actualiza claves y reinicia el proyecto. `.env` queda fuera de Git. Las variables `UPTC_OIDC_ISSUER_URI` y `UPTC_OIDC_AUDIENCE` se dejan vacías por defecto; mientras sigan vacías, `GET /api/v1/me` requiere un token que no está disponible en Compose y responderá 401, y no se puede publicar identidad visual mediante el API. No se incluye una cuenta ni token de prueba.

MySQL escucha solo en `127.0.0.1:3307` y conserva datos en `mysql-data`. `docker compose down` conserva volúmenes; `docker compose down -v` elimina la base y activos locales.

## Validación

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/branding
```

Pruebas frontend: `npm ci`, `npm test`, `npm run build`, `npm run lint` desde `frontend/`. Pruebas backend: `backend\mvnw.cmd verify` en PowerShell o `./mvnw verify` en Git Bash desde `backend/`; las pruebas usan H2. El arranque Compose ejecuta migraciones Flyway sobre MySQL real de desarrollo.

La meta de consulta promedio menor a 50 ms queda pendiente de un conjunto de carga, volumen y percentiles aprobados; este entorno no constituye evidencia de rendimiento institucional.
