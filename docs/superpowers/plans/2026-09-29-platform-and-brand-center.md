# Plataforma base y Centro de Identidad Visual — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar el primer incremento verificable de la plataforma institucional con Vite/React, Java 25/Spring Boot, MySQL y un centro seguro para administrar identidad visual UPTC.

**Architecture:** Un monorepo contiene dos monolitos desplegables de forma independiente. La SPA consume un contrato REST versionado; el monolito Spring Boot separa dominio, casos de uso y adaptadores; MySQL conserva versiones y auditoría; un puerto almacena imágenes fuera de la base.

**Tech Stack:** Java 25 Temurin por SDKMAN; Spring Boot 4.1.1; Maven Wrapper; MySQL; Flyway; React; Vite; TypeScript; Vitest; JUnit.

**Spec:** `docs/specs/phase-1-platform-and-brand-center.md`

## Global Constraints

- Frontend Vite/React/TypeScript en `frontend/`; backend Java 25/Spring Boot en `backend/`; no mezclar reglas de negocio en React.
- Un solo backend monolítico modular y una base MySQL; no introducir microservicios, colas ni bases separadas.
- SDKMAN fija la versión `25.0.4-tem` en `.sdkmanrc`; Maven Wrapper fija su runtime de build.
- Migraciones Flyway explícitas; producción nunca usa `ddl-auto=create` ni `ddl-auto=update`.
- Toda función nueva comienza con prueba AAA, RED observado, GREEN observado y refactor; incluir felices, bordes, errores y permisos.
- No usar ni capturar datos personales reales; no publicar endpoints administrativos sin autorización de servidor.
- Las imágenes aceptadas son PNG/JPEG/WebP con validación de firma, tamaño y dimensiones; no SVG ni nombre/ruta proporcionados como clave interna.
- Paleta base oficial documentada: `#FFCC29` y `#1A1A1A`; el usuario administrador puede modificarlas con validación y previsualización.
- El objetivo `<50 ms` es una meta de medición, no una promesa hasta ejecutar carga representativa.

## Review Focus

- Usuario sin permiso que invoca directamente el endpoint de publicación debe recibir 401/403 y no producir cambios ni eventos de publicación.
- Actualización concurrente basada en revisión antigua debe recibir 409 sin sobrescribir la versión vigente.
- Hex inválido, contraste insuficiente, fecha invertida, módulo desconocido o texto alternativo vacío no deben activar una publicación incompleta.
- Archivo de contenido falso con extensión PNG, archivo sobredimensionado, imagen de dimensiones excesivas o ruta con traversal no debe persistirse ni servirse.
- Falla o demora del endpoint público de marca debe usar defaults oficiales accesibles; no debe dejar la aplicación en blanco ni aplicar CSS arbitrario.

---

### Task 1: Toolchain y estructura reproducible

**Files:**
- Create: `.sdkmanrc`, `README.md`, `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig*.json`, `frontend/src/main.tsx`, `backend/pom.xml`, Maven Wrapper y `.env.example`.
- Create: `frontend/src/test/setup.ts`, configuración Vitest, `backend/src/test/resources/application-test.yml` y `tools/use-sdkman-java.ps1`.

**Interfaces:**
- Consumes: `AGENTS.md`, `docs/PROJECT.md`, esta especificación.
- Produces: `frontend/src/lib/api.ts` como cliente HTTP base y raíz Maven `co.edu.uptc.universiry`.

- [ ] Confirmar que `sdk current java` es `25.0.4-tem` y crear `.sdkmanrc` con `java=25.0.4-tem`.
- [ ] Generar el esqueleto Vite React TypeScript sin lógica institucional; agregar scripts `dev`, `build`, `test`, `test:watch`.
- [ ] Generar Maven Wrapper y POM Spring Boot 4.1.1, Java 25, Web, Security, Validation, Actuator, JPA, MySQL, Flyway y pruebas.
- [ ] Verificar los scripts de ejecución listados en `package.json` y POM; no añadir pruebas sin comportamiento de producto.
- [ ] Crear un helper PowerShell invocable que sincronice `JAVA_HOME` y el primer `PATH` del proceso con el `current` de SDKMAN; no modificar PATH global de máquina.
- [ ] Ejecutar `npm run build` y `./mvnw -q -DskipTests package` para verificar solo scaffold y configuración.
- [ ] Commit local `build: scaffold frontend and backend monoliths`.

### Task 2: Dominio de configuración visual

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/branding/domain/BrandingConfiguration.java`, `BrandColor.java`, `BrandModule.java`, `BrandBanner.java`.
- Create: `backend/src/test/java/co/edu/uptc/universiry/branding/domain/BrandingConfigurationTest.java` y pruebas específicas de colores/contraste.

**Interfaces:**
- Consumes: tipos base del Task 1.
- Produces: `BrandingConfiguration.defaults()`, `BrandColor.fromHex(String)`, `BrandingConfiguration.publish(BrandingChange, Actor, expectedRevision)`, `BrandingConfiguration.restore(targetRevision, Actor, expectedRevision)`.
- Rechazos: HEX distinto a `#[0-9A-Fa-f]{6}`, token/claves desconocidas, revisión obsoleta, fechas de banner invertidas y etiquetas vacías.

- [ ] Escribir `publishes_official_defaults_as_revision_one` con AAA y aserciones literales `assertEquals("#FFCC29", colors.get("primary"))`, `assertEquals("#1A1A1A", colors.get("ink"))`, `assertEquals(1, revision)`.
- [ ] Ejecutar el test y confirmar que falla porque `BrandingConfiguration` no existe.
- [ ] Implementar tipos de dominio inmutables mínimos con defaults oficiales y catálogo cerrado de módulos.
- [ ] Escribir y correr `rejects_non_hex_color`, `rejects_stale_revision`, `rejects_blank_module_label`, `rejects_unknown_module_key`, `blocks_text_pair_below_wcag_aa`; comprobar que 4.49:1 se bloquea y 4.50:1 se permite.
- [ ] Escribir `restoring_old_snapshot_creates_next_revision`; con revisión vigente 8 y objetivo 3, comprobar revisión nueva 9 y contenido idéntico al snapshot 3.
- [ ] Verificar GREEN y suite backend; refactorizar sin introducir dependencia de Spring en el dominio.
- [ ] Commit local `feat: add branding configuration domain`.

### Task 3: Persistencia MySQL, API y permisos

**Files:**
- Create: `branding/application/BrandingService.java`, `BrandingRepository.java`, DTOs, controladores, adaptador JPA, `security/SecurityConfiguration.java`.
- Create: `backend/src/main/resources/db/migration/V1__branding_configuration.sql` para snapshot actual, tokens, etiquetas, banners, activos y auditoría.
- Create/modify: pruebas de servicio, repositorio y MockMvc bajo `branding/`.

**Interfaces:**
- Consumes: dominio del Task 2.
- Produces: `GET /api/v1/branding`, `GET/PUT /api/v1/admin/branding`, `POST /api/v1/admin/branding/rollback`; port `BrandingRepository`; actor desde principal autenticado.
- `PUT` recibe `expectedRevision`; el resultado inserta snapshot inmutable nuevo, mueve puntero singleton y evento de auditoría en una transacción. `rollback` copia un snapshot anterior en revisión nueva.

- [ ] Escribir tests MockMvc `returns_only_public_branding_fields` (HTTP 200, no actor ni auditoría), `includes_etag_from_revision` (ETag estable), `rejects_anonymous_branding_update` (401), `rejects_user_without_brand_admin` (403), `publishes_change_for_brand_admin` (200 y revisión incrementada una vez).
- [ ] Ver cada test fallar por endpoint/permiso inexistente; no escribir el controlador antes de esa señal RED.
- [ ] Escribir test de servicio contra fake real de repositorio para verificar control de revisión y evento en el mismo caso de uso.
- [ ] Escribir test que verifica rollback al snapshot 3 crea revisión 9 cuando la revisión vigente es 8 y jamás altera los snapshots 3 u 8.
- [ ] Verificar `ETag` en API pública, `Cache-Control` corto y 304 para el mismo ETag; banners fuera de vigencia no se publican; restore crea nueva revisión, no modifica snapshots anteriores.
- [ ] Implementar endpoints DTO y autorización `BRAND_ADMIN`; solo GET público de configuración, health check y activos publicados quedan abiertos.
- [ ] Escribir test de integración de migración Flyway/entidades; usar MySQL local cuando la conexión de desarrollo esté provisionada; ninguna prueba de H2 se reporta como verificación MySQL.
- [ ] Ejecutar paquete de pruebas backend y construir `./mvnw verify`.
- [ ] Commit local `feat: persist and secure branding API`.

### Task 4: Servicio de activos de marca

**Files:**
- Create: `branding/application/AssetStorage.java`, `BrandAssetService.java`, validadores y adaptador local bajo `branding/infrastructure/assets/`.
- Create: tests para tamaño/tipo real, dimensiones, traversal, hash, id opaco, autorización, headers y limpieza al fallar la publicación.

**Interfaces:**
- Consumes: identidad y permisos del Task 3.
- Produces: `store(Upload) -> StoredAsset`, `open(AssetId) -> AssetContent`; rutas públicas solo resuelven ids registrados/publicados.

- [ ] Escribir RED para PNG válido servido con `image/png`, `.png` cuyo contenido no es imagen, bytes sobre el límite, dimensiones prohibidas, path traversal y eliminación del temporal si falla MySQL.
- [ ] Implementar almacenamiento generado por UUID fuera de MySQL detrás de interfaz; no aceptar nombre/ruta arbitrarios ni servir SVG.
- [ ] Probar que archivo inválido no deja bytes, fila ni versión publicada; añadir límites configurables y headers seguros.
- [ ] Ejecutar pruebas de activos y suite backend completa.
- [ ] Commit local `feat: validate and store institutional brand assets`.

### Task 5: Cliente de marca y proveedor de tema frontend

**Files:**
- Create: `frontend/src/features/branding/api/brandingClient.ts`, contratos TypeScript, `BrandingProvider.tsx`, `useBranding.ts` y sus tests.
- Modify: entrada de aplicación para hidratar tema público y shell de navegación accesible.

**Interfaces:**
- Consumes: contrato `GET /api/v1/branding` del Task 3.
- Produces: hook `useBranding()` con configuración validada, loading/error/revision; CSS custom properties `--brand-primary`, `--brand-ink`, `--brand-surface`, `--brand-text`.

- [ ] Escribir `applies_api_palette_as_css_variables` con primario literal `#123456`, `uses_official_defaults_when_api_fails` con `#FFCC29`, y `ignores_malformed_untrusted_color` con entrada `url(javascript:alert(1))`.
- [ ] Ver RED correcto antes de crear proveedor.
- [ ] Implementar fetch con esquema runtime, abort/timeouts y fallback literal oficial; nunca insertar estilos/texto sin validación/escape.
- [ ] Ejecutar Vitest y `npm run build`.
- [ ] Commit local `feat: load institution branding in the frontend`.

### Task 6: Centro visual y navegación preparada

**Files:**
- Create: páginas/componentes bajo `frontend/src/features/branding/` para resumen, colores, logos, banners, etiquetas de módulos, vista previa y guardado.
- Create: pruebas Testing Library para permisos UI, errores, validación, cambios locales y guardado.

**Interfaces:**
- Consumes: `useBranding`, cliente admin del Task 3, port `POST /api/v1/admin/branding/assets`.
- Produces: UI con campos accesibles, vista previa aislada, estado sucio, control de revisión y feedback de guardado.

- [ ] Escribir primero `edits_only_preview_until_save`, `edits_known_module_label`, `blank_module_label_blocks_publish`, `banner_requires_alt_text_and_validity`, `conflict_409_preserves_draft`, `rollback_creates_new_revision`; comprobar vista previa `#123456`, sin envío antes de guardar, módulo conocido renombrado, banner con alt y fechas válidas, y draft preservado tras 409.
- [ ] Escribir tests keyboard/semantic queries para navegación, nombre de módulo, banner vigente/vencido y error de carga.
- [ ] Ver RED esperado y desarrollar shell adaptable con identidad UPTC, editor, previsualización y mensajes claros.
- [ ] Ejecutar tests frontend y build de producción; revisar que no haya controles ficticios con datos académicos de producción.
- [ ] Commit local `feat: add visual identity control center`.

### Task 7: Documentación y verificación de extremo a extremo

**Files:**
- Modify: `docs/architecture/c4.md`, `docs/architecture/process-flows.md`, `docs/architecture/data-model.md`, `docs/ROADMAP.md`, README y `.env.example`.
- Create: `docs/runbook/local-development.md` y `docs/security/brand-assets.md`.

**Interfaces:**
- Consumes: contratos/API, configuración de build, resultados reales de pruebas y servicios locales.
- Produces: pasos reproducibles, matriz de configuración, límites de entorno y checklist de corte.

- [ ] Ejecutar suite completa frontend, `npm run build`, `./mvnw verify`, chequeo de migraciones y smoke test local con MySQL real si hay credenciales de desarrollo disponibles.
- [ ] Verificar `GET /api/v1/branding`, rechazo 401/403 de escritura no autorizada y publicación autorizada en integración.
- [ ] Ejecutar un smoke de UI en navegador solo si la aplicación puede arrancar localmente sin acceso a datos institucionales; nunca conectar a producción.
- [ ] Inspeccionar diff, estado Git, secretos, duplicaciones y diagramas; solo entonces reportar evidencia y límites.
- [ ] Commit local `docs: document platform foundation and branding operations`.

## Pasos de ejecución

Cada subpaso nuevo seguirá RED → GREEN → REFACTOR y ejecutará el runner correspondiente antes de pasar al siguiente. Cada tarea tendrá una revisión independiente y un commit local; no se publicará ni se integrará a infraestructura UPTC.
