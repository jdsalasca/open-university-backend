# Universiry — checkout de integración

Plataforma institucional UPTC en desarrollo. El frontend y el backend viven en repositorios privados independientes, con ramas `develop` coordinadas. Este repositorio contiene el backend Spring Boot, `compose.yaml`, documentación y el frontend como submódulo para desarrollo local.

## Clonar y levantar

```powershell
gh repo clone jdsalasca/Universiry-backend
Set-Location Universiry-backend
git switch develop
git submodule update --init --recursive
docker compose up --build -d --wait
```

En una segunda terminal del checkout:

```powershell
docker compose watch --no-up
```

- Frontend: http://localhost:5173
- API de desarrollo: http://localhost:8080
- MySQL local: `127.0.0.1:3307` (solo enlazado a loopback)
- Salud del backend: http://localhost:8080/actuator/health

Los valores de base de datos incluidos son solo para desarrollo local; se pueden reemplazar mediante `.env` ignorado por Git. `docker compose down` detiene el entorno. Los datos de MySQL se conservan en el volumen local `mysql-data`; usa `docker compose down -v` solo si deseas borrarlos.

Compose no configura SSO institucional. La API rechaza cambios administrativos mientras no haya un emisor OIDC/audience y roles autorizados; no usar datos personales reales.

## Catálogo académico v0

La vista previa de programas de **pregrado presencial** está en <http://localhost:5173/#programas>. Permite consultar asignaturas de una versión publicada, filtrarlas por semestre o por código/nombre y recorrerlas en páginas de hasta 100 filas; el API oculta borradores y el catálogo local empieza vacío. La carga CSV, la revisión del borrador y la publicación requieren permisos backend `academic:catalog:read` / `academic:catalog:write`, aún sin mapeo de grupos institucionales en Compose. La ruta de vista previa no significa que el módulo esté habilitado: `programs.available` permanece en `false`.

El contrato de columnas está en [academic-curriculum-template.csv](docs/templates/academic-curriculum-template.csv); el archivo solo contiene encabezados. La pantalla ofrece su descarga mediante un endpoint generado desde el esquema Java, para que el navegador no mantenga una copia de los nombres de columna. Las consultas públicas de detalle reutilizan el mismo modelo normalizado y exigen estado `PUBLISHED`; no agregan tablas. El diseño, las siete tablas y las reglas de cohorte están documentados en [C4](docs/architecture/c4.md), [modelo de datos](docs/architecture/data-model.md) y [flujo de importación/publicación](docs/architecture/process-flows.md). El catálogo no maneja aspirantes ni registros de estudiantes.

## Estructura

- `backend/`: Java 25 y Spring Boot; `.sdkmanrc` fija `25.0.4-tem` para el entorno host.
- `frontend/`: submódulo al repositorio `Universiry-frontend`, Vite, React, TypeScript y SCSS.
- `compose.yaml`: servicios locales frontend, backend y MySQL 8.4 con Compose Watch.
- `docs/`: cronograma, alcance, modelo de datos, procesos, arquitectura C4 y runbooks.

Consulta [el README del backend](backend/README.md) para su arquitectura, comandos SDKMAN y límites operativos.

## Java del host

`.sdkmanrc` fija `25.0.4-tem`. En PowerShell selecciona el candidato SDKMAN solo para esa sesión antes de Maven:

```powershell
. .\tools\use-sdkman-java.ps1
java -version
.\backend\mvnw.cmd -f backend\pom.xml verify
```

Consulta [desarrollo local](docs/runbook/local-development.md) y [el cronograma](docs/ROADMAP.md) antes de integrar cambios.
