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
- Create: `.sdkmanrc`, `README.md`, `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig*.json`, `frontend/src/main.tsx`, `backend/pom.xml`, `backend/mvnw`, Maven Wrapper files y `.env.example`.
- Create: `frontend/src/test/setup.ts`, configuración Vitest, `backend/src/test/resources/application-test.properties` (H2 solo tests), `tools/use-sdkman-java.ps1`.
- Modify generated test: `backend/src/test/java/co/edu/uptc/universiry/UniversiryBackendApplicationTests.java` to activate the test profile for its context-start smoke check.

**Interfaces:**
- Consumes: `AGENTS.md`, `docs/PROJECT.md`, esta especificación.
- Produces: runners de frontend/backend y raíz Maven `co.edu.uptc.universiry`; los clientes HTTP de producto se crean junto con la prueba de su primer consumidor.

- [ ] Confirmar que `sdk current java` es `25.0.4-tem` y crear `.sdkmanrc` con `java=25.0.4-tem`.
- [ ] Generar el esqueleto Vite React TypeScript sin lógica institucional; agregar scripts `dev`, `build`, `test`, `test:watch`.
- [ ] Generar Maven Wrapper y POM Spring Boot 4.1.1, Java 25, `spring-boot-starter-webmvc`, Validation, Actuator, JPA, MySQL, Flyway y pruebas; dejar Spring Security para el Task 3.
- [ ] Verificar scripts; conservar únicamente el `contextLoads` generado como smoke de arranque con perfil test; no añadir tests frontend sin comportamiento de producto.
- [ ] Crear un helper PowerShell invocable que sincronice `JAVA_HOME` y el primer `PATH` del proceso con el `current` de SDKMAN; no modificar PATH global de máquina.
- [ ] Ejecutar `npm run build` desde `frontend/` y `./backend/mvnw -f backend/pom.xml test` desde la raíz; expected: build Vite y `contextLoads` terminan con código 0 usando H2 de prueba.
- [ ] Commit local `build: scaffold frontend and backend monoliths`.

### Task 2: Dominio de configuración visual

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/branding/domain/BrandingConfiguration.java`, `BrandColor.java`, `BrandModule.java`, `BrandBanner.java`.
- Create: `backend/src/main/java/co/edu/uptc/universiry/branding/application/BrandingQueryService.java`, `branding/infrastructure/web/BrandingController.java`.
- Create: `backend/src/test/java/co/edu/uptc/universiry/branding/domain/BrandingConfigurationTest.java` y `backend/src/test/java/co/edu/uptc/universiry/branding/infrastructure/web/BrandingControllerTest.java`.

**Interfaces:**
- Consumes: tipos base del Task 1.
- Produces: `BrandingConfiguration.defaults()`, `BrandColor.fromHex(String)`, `BrandModule.defaultCatalog()` y public read-only `GET /api/v1/branding`.
- Rechazos: HEX distinto a `#[0-9A-Fa-f]{6}`; los cambios, revisiones, fechas y etiquetas se implementan junto con el caso de uso del Task 3.

- [ ] Escribir primero `returns_official_branding_configuration` como MockMvc contra el esqueleto con H2 de test; aserciones: HTTP 200, `colors.primary == "#FFCC29"`, `colors.ink == "#1A1A1A"`, `revision == 1`.
- [ ] Ejecutar `./mvnw -Dtest=BrandingControllerTest test` desde `backend/`; expected: aserción HTTP falla por 404 porque la ruta aún no existe, no error de compilación.
- [ ] Implementar mínimo `BrandingConfiguration`, `BrandColor`, `BrandModule`, `BrandBanner`, consulta por defecto y GET público para obtener GREEN; todavía no agregar comandos ni persistencia.
- [ ] Después del GREEN inicial, escribir `rejects_non_hex_color`; comprobar `BrandColor.fromHex("javascript:alert(1)")` lanza `IllegalArgumentException` y no produce valor CSS.
- [ ] Escribir `default_catalog_has_stable_keys`; comprobar claves literales `home`, `students`, `programs`, `curricula`, `subjects`, `academic-load`, `visual-identity` sin duplicados.
- [ ] Ejecutar `./mvnw -Dtest=BrandingControllerTest,BrandingConfigurationTest test` desde `backend/`; expected: todas las pruebas pasan y el paquete domain no importa Spring.
- [ ] Commit local `feat: add branding configuration domain`.

### Task 3: Persistencia MySQL, API y permisos

**Files:**
- Create: `branding/application/BrandingService.java`, `BrandingRepository.java`, DTOs, controladores, adaptador JPA, `security/SecurityConfiguration.java`.
- Add after the security test RED: `spring-boot-starter-security` and `spring-boot-starter-security-oauth2-resource-server`.
- Create: `backend/src/main/resources/db/migration/V1__branding_configuration.sql` para snapshot actual, tokens, etiquetas, banners, activos y auditoría.
- Create/modify: pruebas de servicio, repositorio y MockMvc bajo `branding/`.

**Interfaces:**
- Consumes: dominio del Task 2.
- Produces: `BrandingService.publish(BrandingChange, Actor, expectedRevision)`, `BrandingService.restore(targetRevision, Actor, expectedRevision)`, `GET/PUT /api/v1/admin/branding`, `POST /api/v1/admin/branding/rollback`; port `BrandingRepository`; actor desde principal autenticado; el GET público de Task 2 lee revisión persistida.
- `PUT` recibe `expectedRevision`; el resultado inserta snapshot inmutable nuevo, mueve puntero singleton y evento de auditoría en una transacción. `rollback` copia un snapshot anterior en revisión nueva.

- [ ] Escribir `branding_schema_is_available_after_flyway_migration`; contra H2 de test consulta metadatos y exige siete tablas: revisión, puntero, tokens, módulos, banners, activos y auditoría.
- [ ] Ejecutar `./mvnw -Dtest=BrandingSchemaTest test` desde `backend/`; expected: aserción de metadatos falla porque aún no existen las tablas.
- [ ] Implementar migración Flyway y el adapter de lectura persistida; repetir el test, expected: las siete tablas existen y la revisión inicial contiene `#FFCC29/#1A1A1A`.
- [ ] Escribir tests MockMvc `anonymous_can_read_public_branding` (200 aun sin principal), `publishes_change_for_brand_admin` (HTTP 200 y revisión +1), `anonymous_update_returns_json_401`, `rejects_user_without_brand_admin` (403), `rejects_stale_revision` (409), `rejects_unknown_module_key` (400), `rejects_invalid_hex_and_blank_label` (400), `blocks_text_contrast_below_4_5` (4.49 falla y 4.50 pasa), `restores_snapshot_into_new_revision` (vigente 8/objetivo 3 crea 9 y deja 3 y 8 inmutables), `audit_event_commits_with_publication` (un evento con actor/revisión) e `includes_etag_from_revision` (ETag estable).
- [ ] Ejecutar los tests recién escritos contra el API de solo lectura; expected: comandos devuelven 404 y el esquema inicial existe, antes de agregar el API de escritura.
- [ ] Implementar `BrandingRepository`, comando de publicación y rollback, transacción MySQL, control optimista, evento audit append-only y DTOs.
- [ ] Añadir Spring Security y resource server JWT; autorizar únicamente `BRAND_ADMIN`/`INSTITUTIONAL_ADMIN`, permitir GET público, salud y activos publicados; mapear roles desde claim configurable y fallar cerrado sin claim/rol.
- [ ] Repetir los tests MockMvc; expected: 401/403/409/400 según contrato, publicación autorizada incrementa una vez y rollback copia valores sin mutar revisiones previas.
- [ ] Verificar `ETag`, `Cache-Control` público corto, 304 para el mismo ETag y exclusión de banners vencidos; expected: caché versionada y contenido vigente únicamente.
- [ ] Ejecutar `./mvnw verify` desde `backend/`; expected: suite y build terminan con código 0.
- [ ] Si se provisiona credencial de desarrollo MySQL, ejecutar aparte el smoke de migraciones en MySQL 8; H2 nunca cuenta como verificación MySQL.
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

- [ ] Ejecutar suite completa frontend, `npm run build` desde `frontend/`, `./mvnw verify` desde `backend/`, chequeo de migraciones y smoke test local con MySQL real si hay credenciales de desarrollo disponibles.
- [ ] Verificar `GET /api/v1/branding`, rechazo 401/403 de escritura no autorizada y publicación autorizada en integración.
- [ ] Ejecutar un smoke de UI en navegador solo si la aplicación puede arrancar localmente sin acceso a datos institucionales; nunca conectar a producción.
- [ ] Inspeccionar diff, estado Git, secretos, duplicaciones y diagramas; solo entonces reportar evidencia y límites.
- [ ] Commit local `docs: document platform foundation and branding operations`.

## Pasos de ejecución

Cada subpaso nuevo seguirá RED → GREEN → REFACTOR y ejecutará el runner correspondiente antes de pasar al siguiente. Cada tarea tendrá una revisión independiente y un commit local; no se publicará ni se integrará a infraestructura UPTC.
