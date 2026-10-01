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

La vista previa del catálogo está en `http://localhost:5173/#programas`. El API público es `GET /api/v1/academic-catalog/programs`; al inicio responde `[]` y solo incluye programas con una versión publicada. `GET /api/v1/academic-catalog/programs/{programId}/curricula` lista versiones publicadas. La marca institucional `programs.available` sigue desactivada aunque se pueda abrir la ruta de preview.

El panel administrativo muestra el enlace «Descargar plantilla CSV» incluso sin sesión. Descarga el contrato vigente desde `GET /api/v1/academic-catalog/curriculum-template`, que devuelve un archivo UTF-8 generado desde los encabezados Java y no accede a MySQL.

Con el permiso `academic:catalog:write` en un entorno autorizado, selecciona el CSV y pulsa **Validar CSV** para revisar metadata y una muestra de hasta 10 asignaturas. La ruta `POST /api/v1/admin/academic-catalog/import-previews` vuelve a validar todo el archivo, pero no crea borradores ni eventos. Revisa el resumen y pulsa **Crear borrador**; el servidor recibe y valida nuevamente el archivo antes de guardarlo en una transacción. Cambiar el archivo limpia la muestra anterior. La cola de revisión requiere `academic:catalog:read`, carga 25 borradores por respuesta y acepta un máximo de 100 mediante un cursor de continuación. Crear un borrador reinicia la cola desde el más reciente; publicar vuelve a consultar la posición vigente y reinicia desde el inicio si quedó vacía. La previsualización local continúa sujeta a autenticación y no habilita la carga institucional desde Compose.

## Variables locales

Compose ofrece credenciales sencillas solo para desarrollo. Para cambiarlas, copia `.env.example` a `.env`, actualiza claves y reinicia el proyecto. `.env` queda fuera de Git. Las variables `UPTC_OIDC_ISSUER_URI` y `UPTC_OIDC_AUDIENCE` se dejan vacías por defecto; mientras sigan vacías, `GET /api/v1/me` requiere un token que no está disponible en Compose y responderá 401, y no se puede publicar identidad visual mediante el API. No se incluye una cuenta ni token de prueba.

MySQL escucha solo en `127.0.0.1:3307` y conserva datos en `mysql-data`. `docker compose down` conserva volúmenes; `docker compose down -v` elimina la base y activos locales.

Las conexiones MySQL del backend fuerzan `connectionTimeZone=UTC` y `forceConnectionTimeZoneToSession=true`. Si se define `DB_URL` fuera de Compose, conserva ambos parámetros: las columnas `TIMESTAMP(6)` y los cursores usan una línea temporal UTC sin ambigüedades de horario de verano.

## Validación

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/branding
```

Pruebas frontend: `npm ci`, `npm test`, `npm run build`, `npm run lint` desde `frontend/`. Pruebas backend: `backend\mvnw.cmd verify` en PowerShell o `./mvnw verify` en Git Bash desde `backend/`; las pruebas usan H2. El arranque Compose ejecuta migraciones Flyway sobre MySQL real de desarrollo.

Comprobaciones manuales del stack:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/academic-catalog/programs
Invoke-WebRequest http://localhost:8080/api/v1/academic-catalog/curriculum-template | Select-Object StatusCode,Headers
docker compose exec -T mysql sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -Nse "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank"'
```

La última consulta debe mostrar las migraciones aplicadas, incluida `5`. Para comprobar que la importación administrativa rechaza anónimos sin guardar nada, ejecuta el siguiente smoke test PowerShell con el archivo de encabezados vacío de filas; la respuesta esperada es `401 Unauthorized`:

```powershell
$http = [System.Net.Http.HttpClient]::new()
$form = [System.Net.Http.MultipartFormDataContent]::new()
$csv = [System.IO.File]::ReadAllBytes((Resolve-Path 'docs/templates/academic-curriculum-template.csv'))
$content = [System.Net.Http.ByteArrayContent]::new($csv)
$content.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('text/csv')
$form.Add($content, 'file', 'template.csv')
$response = $http.PostAsync('http://localhost:8080/api/v1/admin/academic-catalog/imports', $form).GetAwaiter().GetResult()
$response.StatusCode
if ($response.StatusCode -ne [System.Net.HttpStatusCode]::Unauthorized) { throw "Esperaba 401 y recibí $([int]$response.StatusCode)" }
$response.Dispose(); $content.Dispose(); $form.Dispose(); $http.Dispose()
```

La plantilla solo declara el contrato de columnas; no contiene oferta académica. Una solicitud autenticada con `academic:catalog:write` sigue requiriendo datos completos válidos y nunca convierte este entorno en un sistema institucional autorizado. Las variables de importación `ACADEMIC_CATALOG_IMPORT_MAX_FILE_BYTES` y `ACADEMIC_CATALOG_IMPORT_MAX_ROWS`, definidas en `.env`, pueden ajustar límites hacia abajo, sin superar los máximos del dominio (2 MiB/10 000 filas); el arranque valida estos topes.

### Contrato y perfil MySQL de paginación curricular

Desde la raíz del repositorio backend puedes repetir el perfil local:

```powershell
.\tools\verify-mysql-curriculum.ps1
```

El script crea un contenedor MySQL 8.4 único, temporal, sin volumen persistente y ligado a un puerto efímero de `127.0.0.1`. Ejecuta contratos de collation/escape, sesión UTC y paginación por cursor mientras se publica otro borrador; valida además que un cierre obsoleto de periodo produzca conflicto y no restaure una revisión de calendario antigua. También mide páginas con 10.000 entradas públicas y 10.000 borradores sintéticos, con 10 calentamientos y 50 muestras por consulta; selecciona Java 25 desde SDKMAN y usa Maven Wrapper. Al terminar elimina solo el contenedor de esa ejecución. No usa ni modifica el contenedor o volumen de MySQL de Compose.

Cuatro ejecuciones del 30 de septiembre de 2026 entre las 20:11 y 20:29 (UTC-5), con Java 25.0.4 de SDKMAN y el perfil indicado, mantuvieron las doce medias bajo el gate local `<50 ms` (rango: 10,964–25,258 ms). Hubo p99 de 170,006 ms en la cola de borradores en la primera corrida y 175,024 ms en búsqueda en la segunda; no reaparecieron en la tercera corrida con registro GC ni en la cuarta. Como cada escenario solo tiene 50 muestras y el p99 nearest-rank equivale aquí a la muestra máxima, esos p99 reflejan una observación aislada, no una estimación estable de la cola. El registro GC mostró pausas G1 de aproximadamente 4,6–7,7 ms, sin explicar esos outliers; su causa sigue sin establecerse. Corridas anteriores ya habían mostrado búsqueda hasta p95/p99 de 108,218/126,693 ms. La tabla completa está en [la especificación del perfil](../superpowers/specs/2026-09-30-curriculum-mysql-search-performance.md). Estas mediciones acreditan solo esta máquina y concurrencia 1. La carga y latencia representativas de UPTC, el hardware destino y el SLO institucional siguen pendientes de acuerdo con los responsables.
