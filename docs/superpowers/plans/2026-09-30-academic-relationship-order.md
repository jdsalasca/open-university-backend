# Academic Relationship Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Persist a display order on each academic-unit and site parent-child relation, then render the hierarchy using that relation order while preserving stable root and program ordering.

**Architecture:** Extend the existing `academics` module and its public snapshot; do not create a parallel hierarchy or order service. Flyway backfills existing relation order from each child node's current display order, JDBC returns the persisted value, and React sorts sibling relations by that value with stable code tie-breakers. Administrative order changes and authenticated editing remain a later slice.

**Scope boundary:** This slice defines and consumes parent-child order during relation creation. The separate maintenance slice must still add effective-dated close/reassign/reorder commands before any institutional master data is loaded.

**Tech Stack:** Java 25, Spring Boot 4, Spring JDBC, MySQL 8.4, Flyway, React 19, TypeScript, SCSS, Vitest, Maven.

**Spec:** `docs/superpowers/specs/2026-09-30-university-academic-student-lifecycle.md` (sections “Programa y orden de la estructura” and “Criterios de aceptación arquitectónica”); `docs/superpowers/specs/2026-09-30-academic-structure-and-periods-design.md` (section “Diseño / Estructura académica”).

## Global Constraints

- Maintain the two modular monoliths and integrate their `develop` branches.
- Use Flyway for schema changes; do not create schema at application startup.
- Keep `academic_organization_unit` and `academic_site` as separate identities and dimensions.
- Persist order on each parent-child relation; retain node order for roots and affiliation order for programs.
- Keep stable tie-breakers so duplicate display positions do not produce nondeterministic results.
- Use synthetic data only; do not seed institutional faculties, programs, places, or periods.
- Apply TDD in RED-GREEN-REFACTOR order and tests in AAA structure.
- Preserve server-side authorization and the current read-only React experience.
- Keep the curricular semester distinct from a dated academic period.
- Update the data model, process diagram, roadmap, and repository guidance when behavior changes.
- Do not claim the average MySQL target below 50 ms without a representative workload measurement.

## Review Focus

- Existing relations retain their current visible order after migration; backfill from the child node order and verify with a migration test.
- A parent-child relation's order takes precedence over the child's global node order only inside that parent; verify root ordering remains unchanged in the frontend test.
- Programs continue to sort by `AcademicProgramAffiliation.displayOrder`; verify the existing program-order UI test remains green.
- Negative relation order is rejected before persistence, while zero is valid; verify domain and HTTP validation tests.
- Public snapshots include only relations valid on the requested date and carry their order; verify repository/controller integration tests and stable code tie-breakers.

---

### Task 1: Add relation order to domain and write contract

**Files:**
- Create: `backend/src/test/java/co/edu/uptc/universiry/academics/domain/AcademicStructureRelationTest.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/domain/AcademicOrganizationRelation.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/domain/AcademicSiteRelation.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicStructureRelationCommand.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/CreateAcademicStructureRelationRequest.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/DefaultAcademicStructureService.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/persistence/JdbcAcademicStructureRepositoryAdapter.java`
- Create: `backend/src/main/resources/db/migration/V10__academic_structure_relation_order.sql`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/persistence/AcademicStructureSchemaTest.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicStructureControllerTest.java`

**Interfaces:**
- Consumes: existing `AcademicStructureRelationCommand`, `CreateAcademicStructureRelationRequest`, and date-bounded relation records.
- Produces: each organization/site relation has `int displayOrder`; the create request accepts a nonnegative `displayOrder`, defaults an omitted or null JSON value to `0` for v1 compatibility, and passes it through the service.

- [x] **Step 1: Write the failing domain, API, and schema tests**

  Add tests proving both relation records accept order `0` and reject order `-1`, while retaining existing endpoint and interval validation. Extend the controller test to create an edge with `displayOrder: 4` and assert the public snapshot returns `4`. Extend `AcademicStructureSchemaTest` to assert `display_order` exists with nonnegative checks on both relation tables after Flyway migrations.

- [x] **Step 2: Run the tests and verify the expected RED**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureRelationTest,AcademicStructureSchemaTest,AcademicStructureControllerTest test`

  Expected: the relation test does not compile because the records lack `displayOrder`; API and schema assertions fail because relation serialization and V10 columns are absent.

- [x] **Step 3: Implement the relation/request contract**

  Add `displayOrder` to both relation records and the command. Validate `displayOrder >= 0`. Since Jackson supplies null for an omitted record-creator property, use a nullable `Integer` request component and normalize null to zero in the request record's compact constructor before `@Min(0)` validation. Pass the value into both domain relations from `DefaultAcademicStructureService` and update all in-repository direct constructor calls. Add V10 relation columns/checks/indexes, backfill values from child node order, and update JDBC mappers/inserts/selects so order round-trips.

- [x] **Step 4: Run the tests and verify GREEN**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureRelationTest,AcademicStructureSchemaTest,AcademicStructureControllerTest test`

  Expected: domain tests, order round-trip, missing-order compatibility, schema assertions, and existing structure endpoint tests pass.

- [x] **Step 5: Verify schema and compatibility assertions**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureSchemaTest,AcademicStructureControllerTest test`

  Expected: Flyway applies through V10 and both relation tables expose a nonnegative order column; the API returns a relation's persisted value.

- [x] **Step 6: Commit Task 1**

  ```bash
  git add backend/src/main/java backend/src/main/resources/db/migration/V10__academic_structure_relation_order.sql backend/src/test/java
  git commit -m "feat: persist and order academic relations"
  ```

### Task 2: Read and return relationship order deterministically

**Files:**
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/persistence/JdbcAcademicStructureRepositoryAdapter.java`
- Modify: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicStructureControllerTest.java`

**Interfaces:**
- Consumes: relation `displayOrder` from Task 1.
- Produces: public and administrative structure snapshots return relation order; relation creation persists the request value.

- [x] **Step 1: Write the failing persistence/API test**

  Add one test that creates a faculty with two child units whose node orders conflict with the relation orders, creates edges with relation orders `8` and `2`, and asserts the public snapshot places order `2` before order `8`. Add the equivalent assertion for two nested sites. Add an HTTP case where `displayOrder: -1` returns `400` and creates no relation or audit event.

- [x] **Step 2: Run the focused tests and verify the expected RED**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureControllerTest test`

  Expected: values round-trip, but sibling relation order assertions fail because the SQL result sets still sort by child node order.

- [x] **Step 3: Implement JDBC persistence and ordering**

  Order public relation rows by parent node order, parent code, relation order, child node order, then child code. Keep admin history deterministic by parent, child, valid-from, and relation order. Ensure the all-edge graph queries continue to consume the updated mapper and retain cycle detection behavior.

- [x] **Step 4: Run the focused tests and verify GREEN**

  Run: `mvn -f backend/pom.xml -Dtest=AcademicStructureRelationTest,AcademicStructureSchemaTest,AcademicStructureControllerTest test`

  Expected: persistence, ordering, date filtering, cycle, invalid order, and permission tests pass; invalid input leaves no relation or audit row.

- [x] **Step 5: Commit Task 2**

  ```bash
  git add backend/src/main/java backend/src/test/java
  Tasks 1 and 2 share the JDBC adapter and integration-test surface; the execution ruling records them as one backend vertical-slice commit.
  ```

### Task 3: Render sibling order from the relationship in React

**Files:**
- Modify: `frontend/src/features/academics/academicOperationsContracts.ts`
- Modify: `frontend/src/features/academics/academicOperationsClient.ts`
- Modify: `frontend/src/features/academics/academicOperationsClient.test.ts`
- Modify: `frontend/src/features/academics/AcademicOperationsPage.tsx`
- Modify: `frontend/src/features/academics/AcademicOperationsPage.test.tsx`

**Interfaces:**
- Consumes: `displayOrder` on `AcademicOrganizationRelation` and `AcademicSiteRelation` from the API.
- Produces: units and places are rendered by sibling relation order, then child display order, then code; root items still use node display order; programs remain ordered by affiliation display order.

- [x] **Step 1: Write the failing component test**

  Add two child units and two child sites whose node-level order conflicts with their relation-level order. Assert the DOM order follows `relation.displayOrder`, and assert root node and existing program affiliation order are unchanged.

- [x] **Step 2: Run frontend tests and verify the expected RED**

  Run: `npm test -- --run src/features/academics/AcademicOperationsPage.test.tsx`

  Expected: the new sibling-order assertions fail because the tree sorts child nodes by node order only.

- [x] **Step 3: Implement typed parsing and relationship-based sorting**

  Require and validate a nonnegative integer `displayOrder` for both relation types in the API parser. Build child collections as relation/child pairs; sort by relation order, child node order, and code. Apply the same rule independently to the site tree. Do not change program affiliation sorting or root sorting.

- [x] **Step 4: Run focused frontend tests and verify GREEN**

  Run: `npm test -- --run src/features/academics/AcademicOperationsPage.test.tsx src/features/academics/academicOperationsClient.test.ts`

  Expected: relationship-order tests, strict response parsing, and existing empty/error/program/period cases pass.

- [x] **Step 5: Commit Task 3 in the frontend repository**

  ```bash
  git -C frontend add src/features/academics
  git -C frontend commit -m "feat: render academic relationships in configured order"
  ```

### Task 4: Update architecture records and verify both repositories

**Files:**
- Modify: `docs/architecture/data-model.md`
- Modify: `docs/architecture/process-flows.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/specs/2026-09-30-academic-structure-and-periods-design.md`
- Modify: `frontend/AGENTS.md`

- [x] **Step 1: Document the implemented contract**

  Record V10 and the backfill, distinguish root node order from child relationship order and program affiliation order, document stable tie-breakers and the creation API field, and state that changing existing relationships remains a future authenticated maintenance command.

- [x] **Step 2: Run final verification**

  Run from repository root: `mvn -f backend/pom.xml test`

  Run from `frontend/`: `npm test`, `npm run build`, and `npm run lint`.

  Run from repository root: `docker compose config` and `docker compose ps`.

  Expected: all commands exit `0`; backend and frontend stay on `develop`; Compose shows backend/frontend up and MySQL healthy; no official data rows are seeded.

- [x] **Step 3: Commit documentation and submodule pointer**

  In the frontend repository, first run:

  ```bash
  git -C frontend add AGENTS.md src/features/academics
  git -C frontend commit -m "feat: render academic relationships in configured order"
  git -C frontend push origin develop
  ```

  Then in the backend repository, commit the frontend pointer and backend documentation:

  ```bash
  git add frontend docs/architecture/data-model.md docs/architecture/process-flows.md docs/ROADMAP.md docs/superpowers/specs/2026-09-30-academic-structure-and-periods-design.md
  git commit -m "docs: define academic relationship ordering"
  git push origin develop
  ```

  Confirm both remote branches and a clean working tree.

## Execution Handoff

The user explicitly requested continued autonomous implementation in the current checkout; use `superpowers:executing-plans` (native execution) and complete the tasks in order. Do not add an administration UI or expose write controls without an institutional identity provider and approved permission mapping.

## Execution rulings

- Task 1: Ruling: Jackson represents an omitted primitive record-creator property as null and rejects deserialization into `int` — preserve the planned v1 compatibility contract by accepting `Integer` and normalizing null to zero in the request record — cost if wrong: omitted or explicitly null order values become position zero rather than a 400.
- Task 2: Ruling: Tasks 1 and 2 share the same JDBC adapter and controller integration-test surface, so their backend implementation shipped in one vertical-slice commit — cost if wrong: persistence/API round-trip and query ordering cannot be reverted independently.
