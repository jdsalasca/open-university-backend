# Acceso administrativo OIDC — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`; seguir RED-GREEN-REFACTOR y pruebas AAA. Mantener cambios en las ramas `develop` autorizadas.

**Goal:** conectar una sesión OIDC configurable a React y mapear claims a permisos internos solo por configuración explícita del backend.

**Architecture:** React usa `oidc-client-ts` como cliente público con Authorization Code + PKCE; el backend conserva su resource server bearer sin estado, valida issuer/audience y publica los permisos resueltos por `/api/v1/me`. Configuración institucional vacía mantiene las mutaciones cerradas.

**Tech Stack:** React 19, TypeScript, `oidc-client-ts`, Spring Boot 4, Spring Security, JUnit, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-30-oidc-administration-design.md`

## Restricciones globales

- No configurar valores, grupos, permisos, scopes o credenciales UPTC no confirmados.
- No agregar usuario o token de demostración.
- No permitir session cookies ni cambiar el resource server bearer sin estado.
- No almacenar tokens en `localStorage`, habilitar refresh token ni imprimir claims/token.
- El mapa de permisos vacío debe entregar cero autoridades.
- React es UX; el servidor aplica la autorización final.

## Review Focus

- Variables faltantes o URL malformada: mantener sign-in deshabilitado y todas las mutaciones denegadas.
- Claim con rol no configurado o permiso parecido/desconocido: producir cero permisos, sin fall-through.
- Respuesta inválida o timeout de `/api/v1/me`: no mostrar controles administrativos.
- Error/cancelación en callback OIDC: limpiar parámetros sensibles y no dejar sesión parcial.
- Dos valores de rol configurados con una misma entrada ambigua: rechazar configuración al iniciar backend.

---

### Task 1: Mapa de roles y permisos de backend sin valores asumidos

**Archivos:**
- Modificar `backend/src/main/java/co/edu/uptc/universiry/security/ApplicationAuthoritiesConverter.java`.
- Modificar `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java`.
- Modificar/pruebas `backend/src/test/java/co/edu/uptc/universiry/security/ApplicationAuthoritiesConverterTest.java` y `SecurityConfigurationTest.java` si existe.

**Contrato:** un mapa JSON `String -> List<String>` es inyectado en el converter; mapa vacío genera cero permisos; claims comparados por igualdad exacta; solo `ApplicationPermission.authority()` es legal.

- [x] **Step 1: Escribir pruebas AAA para mapa vacío, mapeo exacto, rol no mapeado, claim ausente/malformado, permiso desconocido y JSON inválido.**
- [x] **Step 2: Ejecutar las pruebas focalizadas y verificar RED por el contrato/mapeo faltante.**
- [x] **Step 3: Implementar parseo validado de `UPTC_OIDC_ROLE_PERMISSION_MAPPING`, con default `{}` y sin wildcard.**
- [x] **Step 4: Ejecutar pruebas focalizadas en GREEN y confirmar que `/api/v1/me` no expone permisos nuevos sin configuración.**
- [ ] **Step 5: Commit backend con el mapa deny-by-default.**

### Task 2: Cliente `/api/v1/me` e interfaz de identidad

**Archivos:**
- Crear `frontend/src/features/identity/identityContracts.ts`.
- Crear `frontend/src/features/identity/identityClient.ts` y tests.

**Contrato:** `IdentityClient.current(accessToken, signal?) -> {subject, permissions}`; valida JSON, sujeto acotado, lista de permisos permitidos y estado HTTP; nunca lee claims del navegador.

- [x] **Step 1: Escribir pruebas de respuesta válida, HTTP 401/403/5xx, JSON malformado, permiso desconocido y abort.**
- [x] **Step 2: Ejecutar prueba focalizada en RED.**
- [x] **Step 3: Implementar contrato y cliente Bearer sin guardar respuestas en storage.**
- [x] **Step 4: Ejecutar pruebas focalizadas en GREEN.**
- [x] **Step 5: Commit frontend** (`10472a9`).

### Task 3: Cliente OIDC público PKCE y ciclo de sesión

**Archivos:**
- Crear `frontend/src/features/identity/oidcConfiguration.ts` con validación de authority/client/redirect/scope.
- Crear `frontend/src/features/identity/IdentityProvider.tsx` y contexto tipado.
- Crear `frontend/src/features/identity/IdentityProvider.test.tsx`.
- Actualizar dependencias/lockfile con `oidc-client-ts`.

**Contrato:** estado discriminado `unconfigured | loading | anonymous | authenticated | error`; `login()`, `logout()`; manager inyectable para pruebas. El usuario se guarda en `sessionStorage`, `response_type=code`, PKCE activo, `automaticSilentRenew=false`, `loadUserInfo=false` y scope mínimo.

- [x] **Step 1: Escribir pruebas AAA para sin-config, config inválida, callback válido/cancelado, estado de sesión, permisos del API y logout.**
- [x] **Step 2: Ejecutar pruebas focalizadas en RED.**
- [x] **Step 3: Añadir dependencia oficial `oidc-client-ts` y construir adapter/manager PKCE con state/nonce y sesión solo de pestaña.**
- [x] **Step 4: Implementar provider que consulta `/api/v1/me`, descarta permisos en errores y limpia un 401/cierre.**
- [x] **Step 5: Ejecutar tests focalizados y suite frontend en GREEN.**
- [x] **Step 6: Commit frontend** (`10472a9`).

### Task 4: Controles de sesión y autorización por permiso en React

**Archivos:**
- Modificar `frontend/src/App.tsx`, `App.test.tsx`, `App.scss`.
- Adaptar `frontend/src/features/branding/VisualIdentityCenter.tsx` y tests.
- Adaptar `frontend/src/features/academics/AcademicCatalogPage.tsx` y tests.

- [x] **Step 1: Escribir pruebas sin sesión, IdP no configurado, lector, administrador del módulo, firma/salida y permisos entre capacidades.**
- [x] **Step 2: Ejecutar pruebas focalizadas en RED.**
- [x] **Step 3: Mostrar sign-in/out y pasar solo permisos `/api/v1/me` a las vistas existentes.**
- [x] **Step 4: Verificar que permiso de marca no desbloquee catálogo y que el acceso sin sesión siga en lectura.**
- [x] **Step 5: Ejecutar tests/build/lint en GREEN y commit frontend** (`10472a9`).

### Task 5: Configuración y diagramas

**Archivos:** `compose.yaml`, `.env.example` si existe, `docs/PROJECT.md`, `docs/ROADMAP.md`, `docs/architecture/c4.md`, `docs/architecture/process-flows.md`, `AGENTS.md`, `frontend/AGENTS.md`, ADR de acceso.

- [x] Documentar variables públicas `VITE_OIDC_*` y servidor `UPTC_OIDC_*`; afirmar que no hay valores reales ni secretos en `.env.example`.
- [x] Añadir el proveedor OIDC como sistema externo en C4 y el flujo de login/consulta de permisos a proceso.
- [x] Registrar el mapa UPTC issuer/grupo/scope como gate con dueño DTIC; el mapa productivo permanece vacío.
- [x] Actualizar cronograma y estados de módulo sin declarar SSO institucional operativo.

### Task 6: Verificación e integración de milestone

- [x] Ejecutar suite Maven (199 tests, 0 fallos, 6 omitidos), suite frontend (113 tests), build/lint, `docker compose config --quiet` y smoke de salud frontend/backend.
- [x] Confirmar configuración vacía como denegación segura con pruebas; no probar contra UPTC ni emitir tokens.
- [ ] Completar revisión independiente, actualizar submódulo después de frontend commit y publicar commits separados a `origin/develop`.
