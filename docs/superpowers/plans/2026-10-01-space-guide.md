# Guía de espacios UPTC — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publicar una guía buscable de sedes, CREAD y cuatro servicios UPTC, con referencias oficiales trazables y búsqueda cartográfica externa iniciada por la persona.

**Architecture:** El monolito Spring Boot sirve una instantánea JSON versionada a través del puerto `PublicSpaceDirectory`; React consume `GET /api/v1/spaces` mediante `SpaceGuideClient` y filtra el pequeño catálogo localmente. El Centro de Identidad Visual administra etiqueta y visibilidad de `spaces`; la instantánea no se persiste en MySQL hasta que UPTC designe dueño y proceso de mantenimiento.

**Tech Stack:** Java 25.0.4 administrado por SDKMAN, Spring Boot, Jackson, Flyway, MySQL 8.4, React, TypeScript, Vite, SCSS, Vitest y JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-01-space-guide-design.md`

## Global Constraints

- TDD con pruebas AAA: observar RED, implementar el mínimo, observar GREEN y ejecutar las suites completas.
- Solo usar datos geográficos y referencias que publiquen páginas oficiales UPTC; cada registro conserva URL, fecha consultada y fecha de actualización cuando esté declarada.
- El catálogo del primer corte tiene 21 registros: seis sedes/seccionales/regionales, once CREAD y cuatro puntos de servicio; no se afirma que sea exhaustivo.
- No recopilar nombres, teléfonos o correos personales; no inferir departamentos que la fuente no indique; no inventar coordenadas, rutas internas u opciones de accesibilidad.
- La búsqueda en mapa externo ocurre solo mediante activación explícita; no se incrusta mapa ni se solicita geolocalización.
- La ruta y los controles administrativos dependen de branding `spaces.available`/`spaces.visible`; ocultar navegación no protege ni bloquea la lectura pública por URL o API.
- No crear tablas de espacio ni hacer trabajo de optimización; preservar `backend/time,uptime,level,tags`, `backend/time,uptime,level,tags.0` y `output/`.
- Mantener ambos repositorios en `develop`, actualizar el submódulo y sincronizar C4, proceso, modelo, roadmap y `AGENTS.md`.

## Review Focus

- Un registro de CREAD sin departamento publicado debe conservar municipio y dirección sin inferir el departamento; cubre Task 1 con un test de fixture.
- Una fuente sin fecha de actualización declarada debe devolver `null` y conservar la fecha de consulta; cubre Task 1.
- Búsqueda con y sin tilde (por ejemplo, `Chiquinquirá`/`chiquinquira`) debe producir los mismos resultados; cubre Task 3.
- Una dirección vacía o solo un detalle de oficina no puede generar un enlace de mapa engañoso; cubre Task 3 y Task 4.
- `spaces.visible=false` debe ocultar navegación sin bloquear carga directa del `/#espacios`; cubre Task 4.

---

### Task 1: Catálogo público tipado y fuente versionada

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/spaces/domain/SpaceLocationKind.java`, `SpaceLocation.java`, `SpaceSource.java`, `SpaceDirectorySnapshot.java`.
- Create: `backend/src/main/java/co/edu/uptc/universiry/spaces/application/PublicSpaceDirectory.java`.
- Create: `backend/src/main/java/co/edu/uptc/universiry/spaces/infrastructure/catalog/ClasspathPublicSpaceDirectoryAdapter.java`.
- Create: `backend/src/main/resources/spaces/public-space-directory.json`.
- Test: `backend/src/test/java/co/edu/uptc/universiry/spaces/domain/SpaceDirectorySnapshotTest.java` y `backend/src/test/java/co/edu/uptc/universiry/spaces/infrastructure/catalog/ClasspathPublicSpaceDirectoryAdapterTest.java`.

**Interfaces:**
- `PublicSpaceDirectory.snapshot() -> SpaceDirectorySnapshot`.
- `SpaceLocation` conserva `id`, `kind`, `name`, `municipality`, `department`, `address`, `locationDetail`, `mapQuery` y `source`.
- `SpaceSource` conserva `label`, `url`, `checkedAt` y `sourceUpdatedAt` nullable.
- `SpaceDirectorySnapshot` contiene `List<SpaceLocation>` y `officialOfficeDirectoryUrl`; exige una lista no vacía e IDs únicos.

- [x] **Step 1: Escribir pruebas AAA fallidas** para los 21 registros, unicidad, tipos, procedencia por registro, actualización opcional, CREAD sin departamento inferido y carga desde classpath.
- [x] **Step 2: Ejecutar pruebas para observar RED**

Run: `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd '-Dtest=SpaceDirectorySnapshotTest,ClasspathPublicSpaceDirectoryAdapterTest' test` desde `backend/`.

Expected: RED por tipos/puerto/adaptador ausentes. La primera corrida dio errores de compilación al faltar esos tipos, que confirma el RED para un dominio nuevo; ver el Ruling del ledger.

- [x] **Step 3: Implementar dominio, puerto, adaptador y JSON** con seis ubicaciones principales, once CREAD y los cuatro puntos de servicio citados por sus páginas oficiales; el adaptador valida la instantánea al arrancar.
- [x] **Step 4: Repetir las pruebas para observar GREEN**

Run: `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd '-Dtest=SpaceDirectorySnapshotTest,ClasspathPublicSpaceDirectoryAdapterTest' test` desde `backend/`.

Expected: PASS para todos los registros y casos borde del catálogo.

- [x] **Step 5: Commit** `feat(spaces): add sourced public location catalog` (`f2ff0ec`).

### Task 2: Endpoint público de lectura y catálogo administrable

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/spaces/infrastructure/web/SpaceDirectoryController.java` y `SpaceDirectoryControllerTest.java`.
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java`.
- Modify: `backend/src/main/java/co/edu/uptc/universiry/branding/domain/BrandModule.java`, `branding/infrastructure/web/BrandingChangeRequest.java` y pruebas asociadas.
- Create: `backend/src/main/resources/db/migration/V16__add_spaces_module_to_branding.sql`.

**Interfaces:**
- `GET /api/v1/spaces -> SpaceDirectorySnapshot` es anónimo y de solo lectura.
- `spaces` se incorpora al catálogo de branding con etiqueta `Guía de espacios`, `available=true`, `visible=true` y orden `70`; las nueve claves estables se validan en solicitudes y se agregan a cada revisión histórica.

- [x] **Step 1: Escribir pruebas AAA fallidas** para GET anónimo 200 y contrato completo, `POST /api/v1/spaces` denegado, otras rutas no habilitadas, catálogo de branding de nueve claves y backfill Flyway V16.
- [x] **Step 2: Ejecutar pruebas para observar RED**

Run: `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd '-Dtest=SpaceDirectoryControllerTest,BrandingConfigurationTest,BrandingControllerTest,BrandingAdministrationControllerTest,BrandingRepositoryIntegrationTest' test` desde `backend/`.

Expected: fallos esperados de ruta, claves/migración ausentes o longitud del request, no errores del entorno.

- [x] **Step 3: Implementar controlador, allowlist GET, clave de módulo y migración aditiva**; nunca consultar el directorio oficial en tiempo de ejecución.
- [x] **Step 4: Repetir pruebas objetivo para observar GREEN**

Run: `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd '-Dtest=SpaceDirectoryControllerTest,BrandingConfigurationTest,BrandingControllerTest,BrandingAdministrationControllerTest,BrandingRepositoryIntegrationTest' test` desde `backend/`.

Expected: PASS; Flyway no elimina ni reescribe revisiones existentes salvo añadir la clave con su procedencia por revisión.

- [x] **Step 5: Commit** `feat(spaces): expose sourced read-only directory` (`0c89c8d`).

### Task 3: Cliente y página pública de búsqueda

**Files:**
- Create: `frontend/src/features/spaces/spaceGuideContracts.ts`, `spaceGuideClient.ts`, `SpaceGuidePage.tsx`, `SpaceGuidePage.scss` y `SpaceGuidePage.test.tsx`.
- Modify: `frontend/src/features/branding/contracts.ts` y pruebas de contrato.

**Interfaces:**
- `SpaceGuideClient.listSpaces(signal?: AbortSignal) -> Promise<SpaceDirectorySnapshot>` valida las formas recibidas y no persiste datos.
- `SpaceGuidePage` recibe `client` por prop; filtra por texto/tipo con normalización de tildes y genera enlace OpenStreetMap solo desde `mapQuery` no vacío.

- [x] **Step 1: Escribir pruebas AAA fallidas** de carga, búsqueda sin tilde, tipo, filtros combinados, resultados vacíos, error/reintento, fuente oficial, URL cartográfica a partir de dirección válida y ausencia de enlace cuando falta dirección.
- [x] **Step 2: Ejecutar pruebas para observar RED**

Run: `npm exec -- vitest run src/features/spaces/SpaceGuidePage.test.tsx` desde `frontend/`.

Expected: fallos de importación/comportamiento porque cliente/página no existen.

- [x] **Step 3: Implementar contrato, cliente con cancelación y página adaptable/teclado**; no enviar solicitudes hasta que la persona active el enlace cartográfico.
- [x] **Step 4: Repetir la prueba focal para observar GREEN**

Run: `npm exec -- vitest run src/features/spaces/SpaceGuidePage.test.tsx` desde `frontend/`.

Expected: PASS en estados felices, búsqueda, bordes y reintento.

### Task 4: Navegación, marca y entrega conjunta

**Files:**
- Modify: `frontend/src/App.tsx`, `App.scss`, `App.test.tsx`.
- Modify: `docs/architecture/c4.md`, `process-flows.md`, `data-model.md`, `docs/ROADMAP.md`, `AGENTS.md` y este plan.

- [x] **Step 1: Escribir pruebas AAA fallidas** para `/#espacios`, etiqueta de branding, ocultar el enlace lateral con `spaces.visible=false` y mantener la ruta directa pública.
- [x] **Step 2: Ejecutar `npm exec -- vitest run src/App.test.tsx` y observar la falla esperada.**
- [x] **Step 3: Añadir ruta lazy `/#espacios`, enlace visible solo cuando branding lo permita y estados de carga/error integrados.**
- [x] **Step 4: Actualizar C4, flujo, modelo de datos, roadmap e instrucciones** indicando fuente versionada, ausencia temporal de persistencia espacial, fuentes, cobertura inicial y gates cartográficos.
- [x] **Step 5: Ejecutar suites completas y verificar Compose**

Run desde `frontend/`: `npm test`, `npm run build`, `npm run lint`.
Run desde `backend/`: `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd '-Dspring.main.banner-mode=off' '-Dlogging.level.root=ERROR' test`.

Expected/results: `npm test` 24 archivos/231 pruebas + 4 checks de presupuesto pasan; `npm run build`, `npm run lint` y el Maven full suite salen 0. Maven reporta 228 pruebas, 0 fallos, 0 errores y 8 contratos MySQL opt-in omitidos; el benchmark queda diferido por prioridad del usuario. Compose MySQL 8.4 está healthy; `GET /api/v1/spaces` devuelve 21 registros, branding ofrece nueve módulos y `http://localhost:5173/#espacios` muestra búsqueda y las 21 fichas.

- [x] **Step 6: Integrar ambos repositorios**. Frontend `feat(spaces): add public searchable space guide` (`08b7884`); backend/documentación `feat(spaces): document guide and sync frontend` (`0a86767`) actualiza el gitlink.
- [x] **Step 7: Revisar CI de ambos repositorios y preservar los archivos no relacionados sin stagear.** CI pasó en [frontend](https://github.com/jdsalasca/Universiry-frontend/actions/runs/36893267441) y [backend](https://github.com/jdsalasca/Universiry-backend/actions/runs/36893280564), ambos para los HEAD publicados en `develop`. Permanecen sin stagear los archivos preexistentes `backend/time,uptime,level,tags`, `backend/time,uptime,level,tags.0` y `output/`.

## Verificación final

Comparar cada requisito de la especificación con su prueba o artefacto; revisar `git diff --check` en ambos repositorios, estado de Compose, SHA local/remoto de `develop`, gitlink y workflows. Se verificó: `git diff --check` limpio antes de integrar; ambos `develop` locales coinciden con `origin/develop`; gitlink `frontend` apunta a `08b7884`; tres servicios Compose siguen arriba y MySQL healthy; ambos workflows terminaron en verde. La revisión final fue auto-revisión del implementador porque no se pueden invocar agentes en este turno. No declarar inventario completo, orientación accesible, rutas interiores ni datos vigentes más allá de las fechas/fuentes registradas.
