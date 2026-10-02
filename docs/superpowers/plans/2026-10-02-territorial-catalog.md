# Catálogo territorial DIVIPOLA MGN 2025 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose a traceable DANE territorial reference snapshot and a DEV-only dependent selector for future university forms.

**Architecture:** A validated domain snapshot is loaded once by a classpath adapter and exposed through two public read-only Spring endpoints. A typed React client and selector consume those endpoints; the DEV admissions lab presents the selector separately from synthetic application data.

**Tech Stack:** Java 25, Spring Boot 4, Jackson, JUnit 5, MockMvc, React, TypeScript, SCSS, Vitest, Testing Library.

**Spec:** `docs/superpowers/specs/2026-10-02-territorial-catalog-design.md`

## Global Constraints

- Use Java 25 selected from the root `.sdkmanrc`; backend remains one Spring Boot monolith.
- Frontend uses Vite, React, TypeScript, SCSS and the existing dependency lockfile.
- Write AAA tests first, observe the expected failure, implement the minimum, and run the full repository checks.
- Design interfaces before adapters; reuse patterns and avoid new MySQL tables for public reference data.
- Keep admissions demo dynamic-imported only in Vite DEV and keep its bundle excluded from production.
- Do not capture real applicant data, store selection, add secrets, or infer UPTC admission rules.
- Update C4, data model, process flow, roadmap, and `AGENTS.md` with verified behavior.

## Review Focus

- Leading zero and code hierarchy corruption: assert exact codes `05`, `001`, `05001` and reject cross-department rows.
- Non-municipality loss or mislabeling: assert `MUNICIPIO`, `ISLA`, and `AREA_NO_MUNICIPALIZADA` stay distinct.
- Invalid, unknown, or unbounded endpoint paths: assert `400`, `404`, anonymous `GET`, and denied mutation.
- Async selector race/stale child choice: assert a department change clears the entity and stale requests are aborted/ignored.
- Provider/network failure and empty search: assert accessible loading/error/retry/empty states and no persistence or business submission.

---

### Task 1: Read-only territorial catalog API

**Files:**
- Create: `backend/src/test/java/co/edu/uptc/universiry/territories/infrastructure/web/TerritorialCatalogControllerTest.java`
- Create: `backend/src/test/java/co/edu/uptc/universiry/territories/domain/TerritorialCatalogSnapshotTest.java`
- Create: `backend/src/test/java/co/edu/uptc/universiry/territories/infrastructure/catalog/ClasspathTerritorialCatalogAdapterTest.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/domain/TerritorialCatalogSource.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/domain/TerritorialDepartment.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/domain/TerritorialEntityType.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/domain/TerritorialEntity.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/domain/TerritorialCatalogSnapshot.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/application/TerritorialCatalog.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/infrastructure/catalog/ClasspathTerritorialCatalogAdapter.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/territories/infrastructure/web/TerritorialCatalogController.java`
- Create: `backend/src/main/resources/territories/divipola-mgn-2025.json`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java`

**Interfaces:**
- Produces `TerritorialCatalog.snapshot(): TerritorialCatalogSnapshot`.
- Produces `GET /api/v1/territorial-catalog/departments` and `GET /api/v1/territorial-catalog/departments/{departmentCode}/entities`.
- Snapshot records use text codes and immutable collections; the entity type enum preserves the three source categories.

- [x] **Step 1: Write AAA tests** for anonymous endpoint responses/counts/source, invalid and unknown department codes, denied POST, immutable/unique codes, valid hierarchy, type/year values, and classpath snapshot counts.
- [x] **Step 2: Run the focused backend tests** and observe missing-route/domain failures before writing production code.
- [x] **Step 3: Add the validated immutable domain snapshot, read port, and classpath adapter** using a minimal, attributed JSON projection of the DANE 2025 feature layers.
- [x] **Step 4: Add the two controller routes and explicit GET allowlist entries**; keep all other operations under deny-by-default security.
- [x] **Step 5: Run focused tests, then backend `mvn verify`** with Java 25 and the configured disposable MySQL contract.

### Task 2: Typed frontend client and dependent selector

**Files:**
- Create: `frontend/src/features/territorial-catalog/territorialCatalogContracts.ts`
- Create: `frontend/src/features/territorial-catalog/territorialCatalogClient.ts`
- Create: `frontend/src/features/territorial-catalog/TerritorialCatalogSelector.tsx`
- Create: `frontend/src/features/territorial-catalog/TerritorialCatalogSelector.scss`
- Create: matching contract, client, and selector Vitest files.

**Interfaces:**
- Produces `TerritorialCatalogClient.listDepartments(signal?)` and `.listEntities(departmentCode, signal?)`.
- Produces `TerritorialCatalogSelector({ client })`; it owns only transient department, entity and search state.

- [x] **Step 1: Write AAA tests** for response parsing, zero-preserving codes, GET credentials policy, abort-signal forwarding, API error propagation, dependent loads, change/reset, accessible search, loading/error/retry/empty states, and source display.
- [x] **Step 2: Run those tests** and observe failures because the contracts/client/component are absent.
- [x] **Step 3: Implement runtime contract parsing and a read-only client** using `credentials: 'omit'` and optional `AbortSignal`.
- [x] **Step 4: Implement the accessible dependent selector** with local normalized search, cancellation on change/unmount, and no write callback or storage.
- [x] **Step 5: Run focused tests, the complete frontend suite, build, and lint.**

### Task 3: DEV admissions preview and architecture records

**Files:**
- Create: `frontend/src/features/admissions/demo/TerritorialCatalogDemo.tsx` and its test.
- Modify: `frontend/src/features/admissions/demo/AdmissionsWorkflowLab.tsx` and its test.
- Modify: `docs/architecture/c4.md`, `docs/architecture/data-model.md`, `docs/architecture/process-flows.md`, `docs/ROADMAP.md`, `AGENTS.md`, and `frontend/AGENTS.md`.

**Interfaces:**
- The new demo tab consumes `TerritorialCatalogSelector` and the production-shaped read-only client; it remains within the dynamically imported admissions demo directory.
- No application-store contract changes; territorial choices are not submitted or persisted.

- [x] **Step 1: Write an AAA integration test** proving the extra tab loads the catalog, is separate from the synthetic inbox/form, and preserves existing keyboard/calendar behavior.
- [x] **Step 2: Observe the new tab test fail** before implementing it.
- [x] **Step 3: Add the clearly labeled DEV-only tab and explanatory notice** without changing application-store data.
- [x] **Step 4: Update C4, API/data contract, process flow, roadmap status, and agent guardrails** with snapshot provenance and limitations.
- [ ] **Step 5: Run complete backend/frontend validation, inspect the local preview, commit frontend then backend/submodule pointer on `develop`, and push both authorized upstream repositories.**
