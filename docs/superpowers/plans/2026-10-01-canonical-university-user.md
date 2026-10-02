# Identidad canónica de usuario — plan de implementación

> **Ejecución:** continuar en línea con `superpowers:executing-plans`; cada tarea sigue RED → GREEN → REFACTOR con pruebas AAA.

**Objetivo:** introducir `user_id` canónico e inmutable en OIDC y autorización, preservando datos heredados y manteniendo separados permisos y estados del ciclo académico.

**Diseño:** `university_user` es el registro mínimo (UUID y fecha de creación); cada `institutional_identity` referencia uno. Roles y otorgantes usan usuarios canónicos; cada evento de auditoría conserva `actor_user_id` y `actor_identity_id`. La resolución OIDC convierte `(issuer, subject)` a `user_id` antes de consultar permisos. La respuesta `/api/v1/me` y el cliente de gestión de roles exponen/consumen ese identificador estable. No se ofrece asociación/merge, no se crean perfiles académicos ni se activa OIDC institucional.

**Spec:** `docs/superpowers/specs/2026-10-01-canonical-university-user-design.md`

## Restricciones globales

- Migración Flyway V21 compatible con H2 en modo MySQL y MySQL 8.4; todos los datos heredados conservan sus asociaciones mediante `user_id = identity_id`.
- No agregar PII, datos seed, passwords, proveedores/grupos configurados ni endpoint de registro o vinculación de cuentas.
- Cada rol y su auditoría permanecen atómicos, con control de versión y fuente institucional; destinatario, otorgante y actor guardan UUID canónico, y la auditoría conserva además el vínculo federado del actor.
- Un usuario con varias vinculaciones OIDC comparte roles, pero no puede concederse ni revocarse un rol a sí mismo usando otro alias.
- OIDC sigue cerrado por defecto. Los perfiles `APPLICANT`, `ADMITTED` y `STUDENT` siguen sin asignación manual ni permisos de dominio no aprobados.
- Frontend obtiene el UUID y permisos de `/api/v1/me`; no decodifica claims ni guarda tokens o datos personales en almacenamiento web.
- Mantener compatible el monolito, Spring Boot, SDKMAN Java 25, MySQL/Flyway, React/Vite/TypeScript/SCSS y la estructura de dos repositorios.

## Revisión de riesgos

- Datos con `issuer_sha256` coincidente pero `issuer` distinto deben seguir provocando detección de colisión y no una asociación silenciosa.
- Una carrera de primera autenticación debe terminar con un único usuario/vínculo y sin filas huérfanas.
- MySQL puede confirmar DDL por sentencia: V21 agrega/backfillea columnas y crea FK antes de retirar las columnas antiguas; el retiro se deja para el final. La prueba parte de V20 en esquema desechable y no se anuncia como migración reversible. Antes de una futura ejecución productiva se requerirá respaldo y ensayo de restauración.
- Un usuario existente con varios vínculos consulta un único conjunto de asignaciones; `GET` con UUID desconocido devuelve `[]` y `POST` con destinatario desconocido responde 404, sin crear usuario.
- Un `userId` del cliente no autoriza por sí solo ninguna operación; las rutas y servicios deben seguir comprobando permisos.

---

### Task 1: Contrato de backfill y migración V21

**Archivos:** crear `CanonicalUserMigrationIntegrationTest`, `CanonicalUserMigrationMySqlContractTest` y `V21__canonical_university_users.sql`.

**Interfaz producida:** V21 crea `university_user(user_id, created_at)`, agrega la FK no única en `institutional_identity`, migra target/grantor/actor canónico de roles y auditorías mediante sus identidades actuales y reemplaza las FK antiguas de asignaciones por FK a usuario. Mantiene `actor_identity_id` y su FK en auditoría para preservar el vínculo autenticado. No cambia IDs de eventos, asignaciones, ámbitos, issuer, subject ni fechas. Primero agrega, backfillea y valida las nuevas columnas/FK; retira las columnas antiguas de identidad de asignaciones al final.

- [x] Escribir una prueba AAA que ejecute Flyway hasta V20, inserte dos identidades y una asignación con sus scopes y auditoría sintéticos, avance a V21 y compruebe los IDs, referencias y restricciones resultantes.
- [x] Ejecutar la prueba y confirmar RED por ausencia de `university_user`/V21.
- [x] Escribir V21 con backfill determinista e índices/FK requeridos para MySQL 8.4 y H2 MySQL mode.
- [x] Ejecutar la prueba y confirmar GREEN; agregar bordes de tabla vacía, validar que las columnas antiguas de asignaciones se retiren y que `actor_identity_id` siga preservado en auditoría.
- [x] Ejecutar el backfill V20→V21 contra un MySQL 8.4 desechable y comprobar IDs heredados, referencias de auditoría y eliminación de columnas antiguas de asignaciones.

### Task 2: Registro federado y directorio canónico

**Archivos:** modificar `RegisteredIdentity`, `IdentityDirectory` y `JdbcIdentityDirectoryAdapter`; ampliar `IdentityPersistenceIntegrationTest`.

**Interfaz producida:** `RegisteredIdentity` conserva `id` de la vinculación y agrega `userId`; `IdentityDirectory` soporta registro/find/search y `userExists(UUID)`. Registrar un principal válido crea usuario y vínculo en una transacción; registrar el par existente es idempotente.

- [x] Escribir pruebas AAA para reintento, emisores distintos, UUID de usuario estable y dos solicitudes concurrentes del mismo par sin usuario huérfano.
- [x] Ejecutar las pruebas y observar fallos esperados por el campo/tabla canónica ausentes.
- [x] Implementar la resolución; si se pierde la carrera de unicidad, recuperar el vínculo ganador y limpiar el usuario provisional solo cuando no tenga vínculos.
- [x] Ejecutar las pruebas dirigidas y el grupo de persistencia.

### Task 3: Modelo y casos de uso de roles por `user_id`

**Archivos:** modificar `RoleAssignment`, `AccessAuditEvent`, `CreateRoleAssignmentCommand`, `RoleAssignmentRepository`, `RoleAssignmentService`, `CurrentIdentityService` y `CurrentIdentitySnapshot`; actualizar pruebas de dominio/servicio.

**Interfaz producida:** asignaciones guardan `targetUserId`/`grantedByUserId`, eventos `actorUserId` y `actorIdentityId`; búsquedas toman UUID; identidad actual entrega roles consultados por `RegisteredIdentity.userId`. Autoconcesión/autorrevocación comparan UUID canónicos. Una búsqueda por UUID desconocido entrega una lista vacía; una orden de asignación a un usuario desconocido produce 404.

- [x] Agregar pruebas AAA para alias distintos del mismo usuario en autoconcesión, autorrevocación, consulta compartida, usuario inexistente y otra persona no relacionada.
- [x] Ejecutar pruebas y confirmar RED en contratos ausentes o comparación por principal OIDC.
- [x] Cambiar los puertos, registros inmutables y servicios con validación de UUID antes de adaptar JDBC.
- [x] Ejecutar pruebas de dominio y aplicación hasta GREEN.

### Task 4: Adaptadores JDBC y autorización efectiva

**Archivos:** modificar `JdbcRoleAssignmentRepositoryAdapter`, `EffectiveAuthoritiesConverter`, `SecurityConfiguration` y pruebas de integración/seguridad; adaptar fixtures y limpieza a `university_user`.

**Interfaz producida:** leer/escribir roles por `user_id`, insertar auditoría con actor canónico y el vínculo autenticado, comprobar su correspondencia en la misma transacción y convertir principal OIDC a usuario antes de calcular autoridades. Los usuarios sin vínculo registrado no obtienen roles locales.

- [x] Escribir pruebas AAA de persistencia compartida entre dos vínculos, separación de usuarios, rollback, revocación versionada y permisos efectivos del alias asociado.
- [x] Ejecutarlas y confirmar RED por las FK/consultas actuales basadas en `identity_id`.
- [x] Implementar adaptadores preparados y transaccionales por `user_id`; mantener ausencia de seed y comportamiento cerrado.
- [x] Ejecutar integración H2 y contrato MySQL 8.4 con una base desechable.

### Task 5: API y frontend de identidad/accesos

**Archivos backend:** modificar `CurrentIdentityResponse`, `IdentityDirectoryEntryResponse`, `RoleAssignmentResponse`, `CreateRoleAssignmentRequest`, `RoleAssignmentController` y pruebas MockMvc. **Archivos frontend:** actualizar contratos/parser de identidad `/me` y `frontend/src/features/access/roleAccessContracts.ts`, `roleAccessClient.ts`, `RoleAccessPage.tsx` y sus pruebas SCSS/React pertinentes.

**Interfaz producida:** `/api/v1/me` entrega `userId` UUID; búsqueda conserva issuer/subject opacos y devuelve `userId`; consulta de asignaciones acepta `userId`; alta acepta `targetUserId`; respuestas verifican `targetUserId`. `subject` se conserva temporalmente en `/me` para compatibilidad.

- [x] Escribir primero pruebas AAA de API para `/me`, contrato de búsqueda/listado/alta, UUID inválido/inexistente, permisos 401/403 y ausencia de email/claims.
- [x] Escribir primero pruebas React/client para UUID válido/inválido, selección, query por `userId`, creación y respuestas inconsistentes.
- [x] Ejecutar cada grupo y confirmar RED por contrato viejo.
- [x] Implementar DTOs, parsing estricto y ajustes de selección sin almacenar tokens ni ampliar permisos.
- [x] Ejecutar las pruebas dirigidas y `npm test`; repetir build y lint final tras los fixtures añadidos.

### Task 6: Documentación e integración del hito

**Archivos:** actualizar `AGENTS.md`, `docs/architecture/c4.md`, `data-model.md`, `process-flows.md`, `docs/ROADMAP.md` y el estado de `docs/superpowers/specs/2026-10-01-identity-and-scoped-access-proposal.md`.

- [x] Documentar `university_user`, la vinculación OIDC, los roles/auditoría por UUID y el límite explícito: no existe asociación de identidades ni perfiles de estudiante activos.
- [x] Ejecutar backend `mvnw verify` con `.sdkmanrc` Java 25 y contratos MySQL; frontend 285 pruebas, 4 presupuestos de bundle, lint y build; confirmar estado HTTP 200 de frontend y health del Compose activo.
- [ ] Después de actualizar el preview en `develop`, confirmar `/api/v1/me` y rutas administrativas sin credenciales respondiendo con denegación, y observar la migración V21 conservando filas en el MySQL local.
- [x] Revisar duplicación, `git diff --check`, modelo/diagramas y el submódulo; corregir antes de integrar.
- [x] Solicitar revisión final del código; corregir con pruebas RED→GREEN el hallazgo de procedencia del actor y repetir verificación completa Java 25/MySQL 8.4.
- [ ] Crear commits del hito, actualizar el puntero del submódulo, integrar/push a ambos `develop` según la autorización del patrocinador y comprobar remoto y Compose.

## Interfaces compartidas — preflight

- Tarea 2 produce `RegisteredIdentity.userId`; Tareas 3–5 lo consumen. Mantener el significado de `id` como identidad OIDC en Tarea 2 y prohibir que los repositorios de roles usen ese ID después de Tarea 3.
- Tarea 3 produce métodos de repositorio por `UUID userId`; Tarea 4 los usa tanto para JDBC como para resolver el principal de Spring Security. Las firmas se fijan antes de adaptar las implementaciones.
- Tarea 5 consume `userId` de búsqueda y `/me`; la API backend devuelve UUID normalizado de la base (`toString()`), frontend acepta mayúsculas/minúsculas UUID y conserva el texto de presentación del subject sin usarlo como clave.
- Tarea 1 reemplaza las FK antiguas; la limpieza e inserts de Tareas 2 y 4 deben borrar primero auditoría/roles/vínculos y luego usuarios.

## Autoría y orden

Implementación nativa en el worktree `codex/canonical-user-id`. El backend se integra antes que el frontend; el submódulo solo apunta al commit frontend verificado. No modificar la base de Compose con pruebas destructivas; la migración/contrato de datos se prueba en esquemas desechables antes de actualizar el preview persistente.
