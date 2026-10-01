# Program Affiliation Reassignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an authorized academic-structure operator move a program to a different unit and/or site on an effective date, atomically preserving the program identity and audit history.

**Architecture:** Add one application command and repository port operation. The JDBC adapter locks the existing structure-control row, checks the expected source version and destination validity, truncates the source affiliation, inserts its successor and audit event in one transaction. The REST controller exposes the operation; the React admin console uses one React Hook Form to select, preview, confirm and submit it.

**Tech Stack:** Java 25, Spring Boot, JDBC, Flyway, MySQL-compatible H2 contract tests, React, TypeScript, React Hook Form, Vite, SCSS, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-01-program-affiliation-reassignment-design.md`

## Global Constraints

- `effectiveFrom` is strictly after the source `validFrom` and within its inclusive interval.
- The source ends on `effectiveFrom - 1 day`; its successor starts on `effectiveFrom` and inherits the source `validThrough`.
- Unit and site targets must be `ACTIVE` and cover the successor's full interval.
- The successor may not overlap another program affiliation; the source being truncated is excluded from that check.
- At least the unit or site must change; order alone remains the existing order endpoint's responsibility.
- Reuse `academic_program_affiliation`, keep program identity unchanged, and write the audit event in the same transaction.
- Server authorization requires `academic:structure:write`; the UI also requires `academic:structure:read`.
- Missing program, affiliation or target returns `404`; stale source, invalid dates, overlap, inactive/out-of-range targets or no destination change returns `409`; malformed request returns `400`.
- Success returns `201` with `{ "id": "<newAffiliationId>" }`; the next Flyway migration is V19.
- Keep all records synthetic; do not configure institutional SSO or populate UPTC master data.

## Review Focus

- A `null` expected end date means the open-ended source is still current; pin it in the stale-version integration test.
- Month-end cutover must be inclusive and leave no day missing or duplicated; assert the day before the effective date.
- Legacy or concurrent overlapping affiliations must not be hidden by excluding the wrong row; test overlap against an affiliation other than the source.
- A failure while recording the audit event must roll back both the source truncation and successor insert; force an audit-write failure in the isolated H2 test schema and restore its constraint in `finally`.
- After an ambiguous network result or `409`, the UI must refresh without retrying the write; assert one API call and one refresh in the form test.

---

### Task 1: Atomic backend reassignment API

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicProgramAffiliationReassignmentCommand.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/ReassignAcademicProgramAffiliationRequest.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicStructureService.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicStructureRepository.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/DefaultAcademicStructureService.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicStructureController.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/persistence/JdbcAcademicStructureRepositoryAdapter.java`
- Create: `backend/src/main/resources/db/migration/V19__reassign_academic_program_affiliations.sql`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicStructureControllerTest.java`

**Interfaces:**
- Produces `UUID AcademicStructureService.reassignProgramAffiliation(UUID programId, UUID affiliationId, AcademicProgramAffiliationReassignmentCommand command, String actorSub)`.
- Repository port: `void reassignProgramAffiliation(UUID programId, UUID affiliationId, AcademicProgramAffiliationReassignmentCommand command, UUID newAffiliationId, String actorSub)`.
- Command fields: `LocalDate expectedValidFrom`, nullable `LocalDate expectedValidThrough`, `LocalDate effectiveFrom`, `UUID organizationUnitId`, `UUID siteId`, `int displayOrder`, `String sourceReference`.
- REST request fields have the same names and limits as the spec; `expectedValidThrough` is required in JSON but nullable. Return the created affiliation ID in the existing `AcademicStructureMutationResponse`.

- [ ] **Step 1: Add the happy-path controller integration test** `authorized_operator_can_reassign_program_affiliation_from_effective_date_without_duplicate_program`. Arrange an open-ended affiliation and different active destination; assert `201`, response ID, old inclusive end, new start/end, same `program_id`, and exactly one `PROGRAM_AFFILIATION_REASSIGNED` audit event.
- [ ] **Step 2: Run the test to verify RED.** From `backend/`, run `.\mvnw.cmd -Dtest=AcademicStructureControllerTest test`. Expected: the authorized request returns `403` because the path falls through to the default deny rule.
- [ ] **Step 3: Add the exact POST security matcher.** Add `/api/v1/admin/academic-structure/programs/*/affiliations/*/reassign` to the `ACADEMIC_STRUCTURE_WRITE` allowlist in `SecurityConfiguration`; rerun the test. Expected: it now reaches MVC and returns `404`, proving the permission matcher no longer blocks the route before the controller exists.
- [ ] **Step 4: Add the command, validated request, application/repository signatures and controller route.** The route is `POST /api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/reassign`; the service generates one successor UUID and returns it.
- [ ] **Step 5: Implement the transactional JDBC operation.** Lock `academic_structure_control`; load by program and affiliation IDs; compare both expected dates including nullable end; require a strictly later effective date inside the old interval; reject unchanged unit+site; validate active target status and full interval containment; exclude the source from overlap checks; update the source end conditionally; insert the successor preserving the old final date; insert one audit event whose entity ID is the successor ID. Let any failed write roll back all three mutations.
- [ ] **Step 6: Add migration V19.** Replace `ck_academic_structure_audit_action` with the same allowlist plus `PROGRAM_AFFILIATION_REASSIGNED`; keep the existing 32-character column limit and all previous actions.
- [ ] **Step 7: Add AAA integration tests before tightening each rule.** Use a parameterized success contract to cover moving only the unit, only the site and both. Also cover null expected end and stale dates, month-end cutover, missing source/target, inactive and insufficiently valid targets, no destination change even with a new order, overlap with another affiliation, malformed fields/order/reference, read-only and anonymous writers, and rollback after an induced audit insert failure. Run the targeted controller test after each red/green slice; expected statuses and unchanged-row/audit assertions must match the spec.
- [ ] **Step 8: Run the complete backend verification.** From `backend/`, run `.\mvnw.cmd verify`. Expected: Maven verify succeeds with all existing tests and the new H2-compatible migration/API tests.
- [ ] **Step 9: Commit backend changes.** Commit only backend source, migration, security and tests as `feat(academics): reassign program affiliations atomically`.

### Task 2: Frontend client and confirmed reassignment form

**Files:**
- Modify: `frontend/src/features/academics/academicOperationsContracts.ts`
- Modify: `frontend/src/features/academics/academicOperationsClient.ts`
- Test: `frontend/src/features/academics/academicOperationsClient.test.ts`
- Create: `frontend/src/features/academics/ReassignAcademicProgramAffiliationForm.tsx`
- Create: `frontend/src/features/academics/ReassignAcademicProgramAffiliationForm.test.tsx`
- Modify: `frontend/src/features/academics/AcademicOperationsPage.scss` only if shared form styles do not cover the confirmation preview.

**Interfaces:**
- Client method `reassignProgramAffiliation(programId: string, affiliationId: string, command: AcademicProgramAffiliationReassignmentCommand, accessToken: string, signal?: AbortSignal): Promise<string>` returns the server-created affiliation ID.
- The form receives programs, full administrative affiliations, units, sites, client, access token, refresh callback and authorization-rejected callback. It uses the selected affiliation snapshot's IDs and expected dates.

- [ ] **Step 1: Add client-contract tests first.** Assert the exact POST URL/body, bearer header, optional abort signal and parsed `id`; run the targeted Vitest file and confirm RED because the method is absent.
- [ ] **Step 2: Implement the typed client method.** Preserve existing API error and authorization handling; rerun the targeted client test and expect GREEN.
- [ ] **Step 3: Add RHF form tests before creating the component.** Cover source selection, destination validation, preview of the inclusive cutover, explicit confirmation, success refresh, conflict refresh without retry, and 403 authorization revalidation; confirm RED because the component is absent.
- [ ] **Step 4: Implement `ReassignAcademicProgramAffiliationForm` with React Hook Form.** Show source and expected interval, destinations, effective date, order and reference; require a changed unit or site; preview source end/new start/inherited end; send once only after confirmation; refresh both structure views after success or conflict; provide clear 400/404/409/connection feedback; do not retain tokens or persist form data.
- [ ] **Step 5: Run focused frontend tests.** Run `npx vitest run src/features/academics/academicOperationsClient.test.ts src/features/academics/ReassignAcademicProgramAffiliationForm.test.tsx` from `frontend/`. Expected: both files pass.
- [ ] **Step 6: Commit frontend changes in the frontend repository.** Commit as `feat(academics): add program reassignment form` on its integration branch.

### Task 3: Protected console integration and architecture records

**Files:**
- Modify: `frontend/src/features/academics/AcademicOperationsPage.tsx`
- Test: `frontend/src/features/academics/AcademicOperationsPage.test.tsx`
- Modify: `docs/architecture/c4.md`
- Modify: `docs/architecture/data-model.md`
- Modify: `docs/architecture/process-flows.md`
- Modify: `docs/ROADMAP.md`
- Modify: `AGENTS.md`

**Interfaces:**
- `AcademicOperationsPage` renders the new form only when `academic:structure:read` and `academic:structure:write` are both present and the administrative snapshot has loaded.
- On success or conflict the existing reload callback refreshes public and administrative structures; the page does not introduce another API/cache source.

- [ ] **Step 1: Add page-level tests first.** Assert the form is absent without either permission and appears with both; success and conflict reload both snapshots; then run `npx vitest run src/features/academics/AcademicOperationsPage.test.tsx` and confirm RED for the missing integration.
- [ ] **Step 2: Integrate the form into the program-affiliations panel.** Pass published programs, snapshot affiliations/units/sites and the established callbacks; keep authorization enforced by the server.
- [ ] **Step 3: Update diagrams and operational docs.** Show the administrative command and read/write boundary in C4; document V19, temporal split and atomic audit in the model; add a Mermaid sequence for preview/confirm/transaction/refresh/conflict in process flows; mark the function as implemented but still gated from production pending UPTC data/SSO approval; add the new audit action and requirement to `AGENTS.md`.
- [ ] **Step 4: Run all frontend checks.** Run `npm test`, `npm run build` and `npm run lint` from `frontend/`. Expected: all commands succeed.
- [ ] **Step 5: Commit page and documentation changes.** Commit frontend integration in the frontend repository as `feat(academics): integrate program reassignment`; commit backend-repository diagrams and docs as `docs(academics): document program reassignment`.

### Task 4: Integrate the milestone and verify the live preview

**Files:**
- Modify root Git submodule pointer: `frontend/`

- [ ] **Step 1: Integrate the frontend and backend task commits into each repository's `develop` branch.** Preserve the separately coordinated repositories and update the backend's frontend submodule pointer only after the frontend `develop` commit exists.
- [ ] **Step 2: Run root Compose verification.** Run `docker compose ps`, request `http://localhost:5173/` and `http://localhost:8080/actuator/health`, and inspect browser-visible `/#academia` behavior. Expected: frontend 200, backend UP, MySQL healthy, and the gated control remains hidden without institutional authorization.
- [ ] **Step 3: Check repository status and commit the submodule pointer.** Confirm both repos are on `develop` with intended changes only, then commit the pointer as `chore: update frontend submodule` and push the authorized milestone commits to their existing `origin/develop` remotes.
