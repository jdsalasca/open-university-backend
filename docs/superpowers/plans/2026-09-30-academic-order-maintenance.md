# Mantenimiento del orden académico — plan de implementación

> **For agentic workers:** Implementar en línea usando `superpowers:executing-plans` y TDD. La sesión tiene autorización del usuario para continuar el proyecto; no delegar.

**Goal:** permitir corregir el orden visible de la estructura académica con permisos, concurrencia optimista y auditoría.

**Architecture:** cinco rutas explícitas comparten un DTO de cambio de orden y la interfaz de aplicación/repository mantiene cada responsabilidad tipada. El adaptador JDBC actualiza solo elementos vigentes, serializa cambios con el control de estructura y registra auditoría en la misma transacción.

**Tech Stack:** Java 25, Spring Boot, JDBC, MySQL/Flyway, JUnit/MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-30-academic-order-maintenance-design.md`

## Restricciones globales

- Monolito Spring Boot; no añadir servicios distribuidos ni dependencias.
- Pruebas AAA primero y RED observado antes del código productivo.
- No usar datos personales ni filas oficiales de la UPTC.
- No exponer cambios anónimos ni crear tokens de demostración.
- Conservar las tablas y datos existentes; migrar esquema de forma aditiva.

## Review Focus

- El valor esperado está obsoleto: responder 409 sin modificar ni auditar.
- Una entidad/relación futura o vencida no recibe reordenamiento del árbol vigente.
- Cualquier método/ruta nueva está explícitamente allowlisted con `academic:structure:write`.
- Una actualización de prioridad y su auditoría siempre se confirman o revierten juntas.
- La transición textual de auditoría no se trunca ni mezcla identificadores proporcionados por el cliente.

## Autorrevisión

La suite inicial reveló que las auditorías de relaciones y afiliaciones no identificaban el vínculo exacto. Se cambió primero la expectativa del test, se observó el fallo y luego el resumen pasó a incluir la pareja padre/hijo o el ID de afiliación; la prueba volvió a verde. La comprobación adicional de unidades inactivas también confirma conflicto sin cambio ni auditoría. No hubo revisor independiente en este pase; la revisión fue hecha por el autor.

---

### Task 1: Corregir prioridades mediante comandos protegidos

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicDisplayOrderCommand.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/ChangeAcademicDisplayOrderRequest.java`
- Create: `backend/src/main/resources/db/migration/V11__academic_structure_order_audit_actions.sql`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicStructureService.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicStructureRepository.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/DefaultAcademicStructureService.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/persistence/JdbcAcademicStructureRepositoryAdapter.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicStructureController.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicStructureControllerTest.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/persistence/AcademicStructureSchemaTest.java`

**Interfaces:**
- `AcademicDisplayOrderCommand(int expectedDisplayOrder, int displayOrder, String sourceReference)` validates nonnegative, bounded priorities and a nonblank reference.
- Controller routes: `PATCH /api/v1/admin/academic-structure/units/{unitId}/order`; `/sites/{siteId}/order`; `/units/{parentId}/children/{childId}/order`; `/sites/{parentId}/children/{childId}/order`; `/programs/{programId}/affiliations/{affiliationId}/order`.
- All routes accept the same request body and return 204 when changed or already at the requested order.
- The service/repository expose explicitly named change methods for the five target types; all receive actor identity.

**Ruling — effective time:** apply presentation-priority changes immediately and preserve the prior/new values in the audit event. The existing public tree is current-state only, and a priority is not an academic affiliation; adding date-effective priority histories now would add a second temporal model without a current query that consumes it. Cost if wrong: a future historical structure query could not read the earlier order directly and would need a versioned priority history built from the audit trail.

- [x] **Step 1: Write AAA integration tests first**

  Add tests for successful changes for all five target types, each read back through `GET /api/v1/academic-structure`; assert audit actor/reference/old-new values. Add stale expected order, missing row, inactive/expired row, validation, idempotent retry, unauthorized and read-only-permission cases. Add the five new audit keys to the schema contract test.

- [x] **Step 2: Run focused tests and observe RED**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureControllerTest,AcademicStructureSchemaTest test`
  Expected: new patch operations are denied or unresolved before implementation; existing checks remain green.

- [x] **Step 3: Implement minimal typed application and persistence contracts**

  Add the command/request, service methods, repository methods, five controller mappings and corresponding `PATCH` allowlist entries. Do not add client authentication behavior.

- [x] **Step 4: Implement atomic JDBC updates and audit migration**

  Under the existing structure lock, check row existence/current validity and expected value, update only the `display_order`, and append a bounded audit summary in the same transaction. Return not-found/conflict without an audit row; avoid an audit event for an idempotent same-order request.

- [x] **Step 5: Run focused tests and observe GREEN**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureControllerTest,AcademicStructureSchemaTest test`
  Result: 24 tests pass with no failures, errors or skips; the explicit inactive-unit guard also passes.

- [x] **Step 6: Run the full backend suite, inspect diff, update architecture and roadmap**

  Run: `mvn -f backend/pom.xml test`
  Result: `BUILD SUCCESS`, 192 tests, 0 failures, 0 errors and 6 skipped. Architecture, roadmap and `AGENTS.md` describe protected audited commands; interface controls, closures and reassignments remain pending.

- [ ] **Step 7: Commit and publish to `develop`**

  Commit backend/docs changes on `develop`, push `origin/develop`, verify a clean tree and confirm the Compose watch runtime has picked up the backend version.
