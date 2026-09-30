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

Con el permiso `academic:catalog:write` en un entorno autorizado, selecciona el CSV y pulsa **Validar CSV** para revisar metadata y una muestra de hasta 10 asignaturas. La ruta `POST /api/v1/admin/academic-catalog/import-previews` vuelve a validar todo el archivo, pero no crea borradores ni eventos. Revisa el resumen y pulsa **Crear borrador**; el servidor recibe y valida nuevamente el archivo antes de guardarlo en una transacción. Cambiar el archivo limpia la muestra anterior. La previsualización local continúa sujeta a autenticación y no habilita la carga institucional desde Compose.

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

Comprobaciones manuales del stack:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/academic-catalog/programs
Invoke-WebRequest http://localhost:8080/api/v1/academic-catalog/curriculum-template | Select-Object StatusCode,Headers
docker compose exec -T mysql sh -lc 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -Nse "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank"'
```

La última consulta debe mostrar las migraciones aplicadas, incluida `4`. Para comprobar que la importación administrativa rechaza anónimos sin guardar nada, ejecuta el siguiente smoke test PowerShell con el archivo de encabezados vacío de filas; la respuesta esperada es `401 Unauthorized`:

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

El script crea un contenedor MySQL 8.4 único, temporal, sin volumen persistente y ligado a un puerto efímero de `127.0.0.1`. Ejecuta pruebas de collation/escape y un perfil de 10.000 entradas sintéticas con 10 calentamientos y 50 muestras, seleccionando Java 25 desde SDKMAN y usando Maven Wrapper. Al terminar elimina solo el contenedor de esa ejecución. No usa ni modifica el contenedor o volumen de MySQL de Compose.

La última ejecución del perfil del 30 de septiembre de 2026 obtuvo medias de 22,062 ms (sin filtro) y 33,807 ms (búsqueda por subcadena); ambas quedan bajo el gate local `<50 ms`. En búsqueda, p50/p95/p99 fueron 33,544/37,983/40,694 ms. Una ejecución repetida anterior midió p99 de búsqueda de 52,894 ms, lo que muestra variación en la cola aunque la media se mantuvo cerca de 33 ms. El resultado acredita esa máquina y ese perfil de concurrencia 1 únicamente. La carga y latencia representativas de UPTC, el hardware destino y el SLO institucional siguen pendientes de acuerdo con los responsables.
