# Curriculum Version Comparison Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a read-only comparison of an uploaded curriculum CSV against the latest published curriculum with the exact same program identity.

**Architecture:** The application service reuses CSV parsing and validation, asks the catalog repository for the latest published reference in one read-only operation, and produces a bounded comparison value. The existing preview API returns that value; a focused React panel renders its two states without changing the draft-creation flow.

**Tech Stack:** Java 25, Spring Boot, JDBC, MySQL 8.4, JUnit 5, React, TypeScript, SCSS, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-02-curriculum-version-comparison-design.md`

## Global Constraints

- Preserve the two independently deployable monoliths and the current preview endpoint.
- Preserve server authorization `academic:catalog:write`; keep preview reads-only and do not create drafts, audit events, migrations, or seeds.
- Reuse parser-normalized subject codes; do not add institutional selection, admission, equivalence, or cohort-conversion rules.
- Keep each example list at ten entries maximum while returning full counts.
- Keep frontend types additive for frontend/backend version skew; if an older backend omits `comparison`, omit the new panel rather than claim there is no reference.
- Use AAA tests and synthetic test records only. Leave C4 unchanged; update the import process flow.

## Review Focus

- Identity collision: a curriculum with the same code but another level, modality, or campus must never become the reference.
- Publication recency: use `publishedAt`, not draft creation time; break equal timestamps by UUID ascending.
- Decimal scale: `3.0` and `3.00` are numerically unchanged.
- Counts versus examples: counts cover every row even when only ten examples are shown.
- Missing reference: must not display empty comparison counts as zero changes.

## Execution Rulings

- Ruling: Start with the existing MockMvc preview contract and assert `NO_REFERENCE` before introducing new Java types. A test that imports a not-yet-created type would fail at compilation instead of showing the missing runtime behavior. Cost if wrong: this focused Spring/H2 test takes longer than a pure unit test.
- Ruling: Keep the comparison inside the existing catalog service and repository port. No C4 boundary changes are needed; the curriculum import process flow will be updated after implementation. Cost if wrong: the comparison could become too coupled to catalog import if future requirements expand it into a separate capability.
- Ruling: Do not query the persisted Harness Graphify graph for this worktree. Harness reports no graph artifacts and points to a different commit; the current work is contained within existing controller, application, repository and UI boundaries. Cost if wrong: a graph query could have revealed a missed dependency.
- Ruling: Continue on the approved curriculum task while Harness still marks it `planning` and its global active task points to unrelated Library work. The first TDD evidence is attached to the curriculum task in its current Harness phase; do not claim or alter the Library task. Cost if wrong: the Harness phase label can lag behind the code until the project task is reconciled.
- Ruling: Use the existing MockMvc controller test as the service-composition test instead of adding a mocked `DefaultCurriculumPublicationServiceTest`; it runs the real parser, service and H2 repository and asserts comparison results with unchanged row/audit counts, while the MySQL contract exercises the repository query. Cost if wrong: a service-only wiring regression may be caught at the controller boundary rather than in a smaller unit test.
- Ruling: Keep comparison component assertions in `AcademicCatalogPage.test.tsx` instead of a separate panel test; lazy loading is part of the page flow being verified. Cost if wrong: a panel-only rendering defect has less isolated failure output.
- Ruling: Do not merge, push or switch branches in this delivery because the current user-provided `AGENTS.md` requires fresh authorization for those operations. Create local milestone commits only. Cost if wrong: remote integration remains pending until the user authorizes it.
- Ruling: Do not claim the curriculum task or advance Harness to validation; the registered Codex agent is already assigned to another task, and Harness rejects the validation phase while this task remains `planning`. Preserve the task and continue the implementation independently. Cost if wrong: Harness will not count validation results until its task assignment/phase is reconciled.
- Ruling: Record verified outcomes in the execution ledger and Harness checkpoint without changing the unrelated active Library task or claiming validation evidence in Harness. Cost if wrong: Harness's task dashboard will lag the local, tested implementation until its ownership and phase are reconciled.

---

### Task 1: Pure comparison model

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/application/CurriculumVersionComparison.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/application/CurriculumVersionComparisonTest.java`

**Interfaces:**
- Consumes `ValidatedCurriculum` and `AcademicCurriculumDetails`.
- Produces `CurriculumVersionComparison.compare(ValidatedCurriculum, AcademicCurriculumDetails)` and `CurriculumVersionComparison.noReference()`.
- Exposes `Status`, `Reference`, `Counts`, and bounded samples. A modified sample carries the normalized subject code and the exact changed field enum values.

- [x] Add AAA test `compares_normalized_codes_and_counts_added_removed_modified_and_unchanged_rows`; assert all four categories and both count equations.
- [x] Run `.\mvnw.cmd -Dtest=CurriculumVersionComparisonTest test` in `backend` and confirm RED.
- [x] Implement the minimal comparison result and classification for the first test; rerun it and confirm GREEN before proceeding.
- [x] Add AAA test `marks_each_changed_field_and_compares_credits_without_decimal_scale`; assert name, credits, semester, order, formation space, component and option group, while `3.0` versus `3.00` is unchanged.
- [x] Run the focused test and confirm RED; add field-level comparison and rerun to confirm GREEN.
- [x] Add AAA test `keeps_full_counts_and_caps_each_category_sample_at_ten`; use more than ten differences in every category and assert exact totals and sample limits.
- [x] Run the focused test and confirm RED; bound only the examples to ten and rerun to confirm GREEN.
- [x] Add AAA test `represents_missing_reference_without_comparison_counts`; assert `NO_REFERENCE`, null reference/counts and empty sample lists.
- [x] Run the focused test and confirm RED; add the no-reference factory and rerun to confirm GREEN.
- [x] Rerun `.\mvnw.cmd -Dtest=CurriculumVersionComparisonTest test` and confirm GREEN.

### Task 2: Exact published-reference lookup and preview composition

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/application/CurriculumProgramIdentity.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/AcademicCatalogRepository.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/DefaultCurriculumPublicationService.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/application/CurriculumImportPreview.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/persistence/JdbcAcademicCatalogRepositoryAdapter.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicCurriculumImportPreviewResponse.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/application/CurriculumVersionComparisonTest.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/persistence/AcademicCatalogMySqlContractTest.java`
- Test: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicCatalogControllerTest.java`

**Interfaces:**
- `CurriculumProgramIdentity` contains `programCode`, `AcademicLevel`, `StudyModality`, and `campusCode`.
- `AcademicCatalogRepository.findLatestPublishedCurriculum(CurriculumProgramIdentity)` returns `Optional<AcademicCurriculumDetails>`.
- Repository SQL filters all four identity fields and `PUBLISHED`, orders by `published_at DESC, curriculum_id ASC`, then loads the selected curriculum's complete entries in the same read-only transaction.
- `CurriculumImportPreview` carries the comparison. `previewCsv` supplies `noReference()` when lookup returns empty; it never invokes `createDraft`.

- [x] Extend the existing AAA controller tests to assert exact-identity composition, `NO_REFERENCE` and `COMPARED` response shapes, and unchanged row/audit counts; preserve endpoint authorization.
- [x] Add an AAA MySQL contract test with synthetic rows for exact identity, latest publication, deterministic UUID tie-break, and draft exclusion.
- [x] Run the new controller assertions RED before implementation and GREEN afterward; the repository contract test also passed against an isolated MySQL 8.4 container.
- [x] Implement the identity record, repository contract, exact-field lookup, response mapping, and preview composition. No schema migration or performance guarantee was added.
- [x] Run the full backend suite and the opt-in MySQL contract suite; see the execution ledger for exact commands and results.

### Task 3: Frontend contract and comparison panel

**Files:**
- Modify: `frontend/src/features/academics/contracts.ts`
- Modify: `frontend/src/features/academics/AcademicCatalogPage.tsx`
- Create: `frontend/src/features/academics/CurriculumVersionComparisonPanel.tsx`
- Create: `frontend/src/features/academics/CurriculumVersionComparisonPanel.scss`
- Test: `frontend/src/features/academics/curriculumVersionComparisonContract.test.ts`
- Test: `frontend/src/features/academics/AcademicCatalogPage.test.tsx`

**Interfaces:**
- Add a discriminated `CurriculumVersionComparison` TypeScript type matching the response exactly. Parse numeric counts, ISO publication time, bounded strings, and at most ten typed samples; enforce null counts only for `NO_REFERENCE`.
- The panel accepts one parsed `CurriculumVersionComparison`; page integration passes it only when present for compatibility with an older backend.

- [x] Add AAA contract tests for both comparison states, invalid counts/sample limits, and an older backend response that omits comparison.
- [x] Add AAA page/component assertions for no-reference messaging, reference metadata/counts, changed fields, sample bounds, and legacy response compatibility; run RED before implementation.
- [x] Implement the parser/types and accessible Spanish panel using existing theme tokens, SCSS conventions and table/card styles.
- [x] Integrate the panel within `CurriculumImportPreviewPanel` without changing file selection, cancellation, permission checks, or the create-draft action.
- [x] Run focused and full Vitest, lint, and production build; see the execution ledger for exact commands and results.

### Task 4: Process documentation and full validation

**Files:**
- Modify: `docs/architecture/process-flows.md`
- Modify: `docs/architecture/data-model.md`
- Modify: `docs/ROADMAP.md`
- Modify: `docs/superpowers/plans/2026-10-02-curriculum-version-comparison.md`
- Create: `docs/superpowers/plans/2026-10-02-curriculum-version-comparison-ledger.md`

- [x] Extend the curriculum import Mermaid flow with exact-identity lookup, deterministic reference selection, `NO_REFERENCE`, comparison samples, and the unchanged explicit draft-creation step.
- [x] Mark this informative comparison as delivered in the roadmap without claiming official curriculum data or institutional acceptance; document the derived/no-migration data model.
- [x] Run `npm test`, `npm run lint`, `npm run build`, and `.\mvnw.cmd test` (Java 25) in the respective repositories. Run the opt-in MySQL contract suite against a separate temporary MySQL 8.4 container.
- [x] Review both repository diffs, verify the Compose preview and health endpoints, check for unrelated changes, and record outcomes in the [execution ledger](2026-10-02-curriculum-version-comparison-ledger.md).
- [x] Commit the frontend milestone locally, advance the backend frontend submodule pointer, and commit the backend milestone locally. Do not switch branches, merge into `develop`, or push until the user gives fresh authorization under the current `AGENTS.md`; verify local SHAs after committing.
