# Universiry Backend

Monolito modular Java 25 / Spring Boot para la plataforma universitaria. Este directorio pertenece al repositorio privado `Universiry-backend`; en el checkout integrado es raíz de backend y convive con `frontend/` como submódulo de `Universiry-frontend` para desarrollo local. Las ramas de integración de ambos repositorios son `develop`.

## Herramientas y verificaciones

La versión host de Java está fijada en `../.sdkmanrc` (`25.0.4-tem`, Temurin) y la administra SDKMAN. En PowerShell, desde este directorio:

```powershell
& ..\tools\use-sdkman-java.ps1
java -version
.\mvnw.cmd verify
```

El script selecciona el candidato `current` de SDKMAN para el proceso PowerShell y no cambia la configuración global de Windows. `mvnw verify` compila y ejecuta las suites del backend; las pruebas de persistencia usan H2 y Compose aporta el smoke de MySQL 8.4.

## Límites de módulos

- `branding`: configuración visual versionada, imágenes, publicación, validación y auditoría.
- `security`: identidad de sesión, JWT y traducción fail-closed de roles técnicos a permisos internos.
- `academics`: catálogo de pregrado presencial, importación/validación CSV, revisiones curriculares, consultas públicas y auditoría propia.
- `students` y los demás dominios universitarios: no implementados; primero requieren descubrimiento y contratos institucionales.

El catálogo conserva programa, asignatura, revisiones inmutables, plan y entradas por cohorte. Flyway `V2__academic_catalog.sql` crea su esquema normalizado de siete tablas sin reutilizar la auditoría de branding. La carga completa se valida antes de una transacción que persiste el borrador y el evento de importación. La publicación condicional `DRAFT → PUBLISHED` y su evento de auditoría comparten transacción.

## API del catálogo

| Método | Ruta | Acceso |
|---|---|---|
| `GET` | `/api/v1/academic-catalog/programs` | Público; solo planes publicados |
| `GET` | `/api/v1/academic-catalog/curriculum-template` | Público; CSV vacío derivado del esquema del backend |
| `GET` | `/api/v1/academic-catalog/programs/{programId}/curricula` | Público; solo versiones publicadas |
| `GET` | `/api/v1/academic-catalog/curricula/{curriculumId}` | Público; metadata raíz sin asignaturas para una versión publicada; borrador/inexistente responde 404 |
| `GET` | `/api/v1/academic-catalog/curricula/{curriculumId}/entries?page=1&pageSize=100&search=&semester=` | Público; página filtrada de hasta 100 asignaturas publicadas; borrador/inexistente responde 404 |
| `GET` | `/api/v1/admin/academic-catalog/drafts` | `academic:catalog:read` |
| `GET` | `/api/v1/admin/academic-catalog/curricula/{curriculumId}` | `academic:catalog:read` |
| `POST` | `/api/v1/admin/academic-catalog/imports` | `academic:catalog:write`; multipart field `file` |
| `POST` | `/api/v1/admin/academic-catalog/curricula/{curriculumId}/publish` | `academic:catalog:write` |

La consulta paginada valida página, tamaño (1–100), texto (máximo 120 puntos de código Unicode) y semestre (1–32767), devuelve conteos filtrados y ordena por semestre/orden original. Estas lecturas no agregan tablas; el frontend solicita la primera página en paralelo con la metadata, cancela peticiones obsoletas y conserva un máximo de 100 filas visibles. Estos permisos son contratos internos, no nombres de grupos ni roles oficiales de UPTC. Compose no tiene proveedor OIDC ni usuarios de prueba; por ello, un desarrollador anónimo puede consultar un catálogo vacío pero no importar o publicar. La vista React `/#programas` es preview local, y `programs.available` permanece `false`.

## Integración local y documentos

Desde el checkout backend, `docker compose up --build -d --wait` levanta frontend, backend y MySQL local. Sigue [el runbook de desarrollo](../docs/runbook/local-development.md) para Compose Watch, salud, Flyway y smoke de autorización. Arquitectura y procesos: [C4](../docs/architecture/c4.md), [modelo de datos](../docs/architecture/data-model.md), [flujo de importación](../docs/architecture/process-flows.md) y [cronograma](../docs/ROADMAP.md).

No se incluyen datos reales de estudiantes ni registros de programas oficiales. El objetivo de MySQL promedio `<50 ms` requiere una carga y volumen aprobados y mediciones p50/p95/p99; no se considera alcanzado por la suite de pruebas ni por el catálogo vacío.
