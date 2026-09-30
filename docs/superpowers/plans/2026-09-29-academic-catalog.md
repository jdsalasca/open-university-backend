# Academic Catalog Import and Versioning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a secure, versioned pregrado-presencial academic catalog that imports approved curriculum plans from validated CSV drafts and publishes immutable versions by cohort.

**Architecture:** A modular `academics` backend capability owns programs, subject revisions, curriculum versions, import validation, audit and publication. A React catalog page reads published records and exposes protected draft/import actions; Spring Security protects every write, while production UPTC role mapping remains fail-closed until provided.

**Tech Stack:** Java 25 / Spring Boot 4.1.1 / Spring JDBC / Flyway / MySQL 8.4 / Apache Commons CSV 1.14.1 / React 19 / TypeScript / Vite / SCSS / Vitest / JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-29-academic-catalog-design.md`

## Global Constraints

- Keep the backend and frontend as separate monolith repositories integrated on `develop`; the frontend remains a backend-repo submodule for local Compose.
- Use SDKMAN Java 25 (`25.0.4-tem`) and Maven Wrapper; install/resolve backend dependencies through the host SDKMAN/Maven workflow as well as Compose Watch.
- Use MySQL and additive Flyway migrations; never edit the already-applied V1 migration or create production schema at application startup.
- Follow TDD RED → GREEN → REFACTOR with AAA tests; do not implement before observing the matching failure.
- No real student/application data, institutional credentials, or public role mapping in fixtures/configuration.
- All new `/api/v1/admin/**` behavior must be explicitly allowlisted by method and path; public reads return published catalog data only.
- Preserve historic program/subject revisions and curricula; publication is immutable and audit is atomic.
- Keep the current `<50 ms` average SQL objective unclaimed until tested with representative data and load, reporting p50/p95/p99 as well.

## Review Focus

- UTF-8/BOM, quoted commas, duplicate/missing/extra headers, invalid encoding and inconsistent repeated metadata must return deterministic row/field errors and persist nothing.
- Duplicate course codes within a plan and duplicate `(program, version)` imports must never silently create duplicate rows.
- Reusing a code with changed title/credits must create a new subject revision and leave previously published curricula unchanged.
- Concurrent/repeated publication must produce one state transition and one audit record; failed publication must leave the draft intact.
- Anonymous, read-only and unknown-role requests must not import or publish; unregistered methods/routes under the administration prefix must stay denied.
- Max file/row limits and very long fields must fail before persistence without logging file content or file names.

---

### Task 1: Domain contracts and validation

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/domain/AcademicProgram.java`, `AcademicProgramRevision.java`, `AcademicLevel.java`, `StudyModality.java`, `AcademicSubject.java`, `AcademicSubjectRevision.java`, `AcademicCurriculum.java`, `AcademicCurriculumEntry.java`, `AcademicCurriculumStatus.java`, `AcademicCatalogLimits.java`, and shared domain-only `AcademicCatalogValueRules.java`.
- Create: `backend/src/test/java/co/edu/uptc/universiry/academics/domain/AcademicCurriculumTest.java`, `AcademicSubjectRevisionTest.java`, and `AcademicProgramTest.java`.

**Interfaces:**
- `AcademicProgram` identity is `(programCode, academicLevel, studyModality, campusCode)`; v1 accepts only `PREGRADO` and `PRESENCIAL`; revisions contain SNIES code (nullable), display name, faculty and campus name.
- `AcademicSubject` identity is `subjectCode`; `AcademicSubjectRevision` is immutable `(subjectId, subjectName, credits)`.
- `AcademicCurriculum` contains program id/revision id, version label, cohort start/end, approval reference, status and immutable entries.
- `AcademicCurriculumEntry` contains subject id and its subject-revision id (validated as a matching pair), semester number, exact formation-space/component labels, optional choice-group and source row order. A curriculum rejects repeated subject identity even if different revisions were supplied.

- [x] **Step 1: Add only immutable type signatures/value shapes** (no business validation) so tests compile and the domain interface is explicit before adapters.
- [x] **Step 2: Write failing AAA tests** for blank/oversized identities per the field limits in the spec, required v1 level/modality, nonpositive/overprecision/oversized credits, invalid cohort format/range, empty curriculum, duplicate subject identity across revisions, mismatched subject/revision identity, and valid electives/group labels. Unsupported CSV level/modality values are covered in Task 2.
- [x] **Step 3: Run `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd -Dtest=AcademicCurriculumTest,AcademicSubjectRevisionTest,AcademicProgramTest test` from `backend/`.** Confirm failures are behavior assertions, not compilation/setup errors.
- [x] **Step 4: Implement immutable domain values and `AcademicCurriculumStatus` (`DRAFT`, `PUBLISHED`) with validation; add no Spring/JDBC imports to domain.**
- [x] **Step 5: Rerun targeted tests, then the backend unit suite; refactor only while green.**
- [x] **Step 6: Commit `feat: model versioned academic catalog`.**

### Task 2: CSV v1 parser and whole-file validation

**Files:**
- Modify: `backend/pom.xml` to add `org.apache.commons:commons-csv:1.14.1`.
- Create: `academics/application/CurriculumCsvParser.java`, `CurriculumCsvException.java`, and `CurriculumImportService.java`.
- Create: `academics/application/ParsedCurriculum.java`, `ValidatedCurriculum.java`, and typed source-row/program/entry value types needed by these interfaces.
- Create: `academics/infrastructure/csv/ApacheCommonsCurriculumCsvParser.java` and `academics/infrastructure/csv/ApacheCommonsCurriculumCsvParserTest.java`.
- Create: `academics/application/CurriculumImportServiceTest.java`.

**Interfaces:**
- `CurriculumCsvParser.parse(InputStream) -> ParsedCurriculum` enforces `AcademicCatalogLimits.MAX_IMPORT_BYTES` while reading and parses the exact CSV v1 header from the spec; it never trusts only request `Content-Length`. Reuse the shared domain limits rather than duplicating field/row bounds.
- `CurriculumImportService.validate(ParsedCurriculum) -> ValidatedCurriculum` checks all metadata consistency, row-level fields, unique subject codes, cohort range and configured row bound before any persistence call. Parser applies byte bound while reading.
- Validation exceptions contain safe row/column identifiers only, never raw input values or original file name.

- [x] **Step 1: Write RED tests** for BOM, quoted commas/UTF-8 accents, valid multi-semester records, missing/unknown/duplicate headers, malformed UTF-8, empty file, inconsistent repeated metadata (including academic level/modality/campus), unsupported levels/modes, blank required fields, overlong fields, nonpositive/overprecision credits, invalid semester, duplicate subject code, reversed cohort range, a stream over 2 MiB with a dishonest size header, and >10,000 rows.
- [x] **Step 2: Run `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd -Dtest=ApacheCommonsCurriculumCsvParserTest,CurriculumImportServiceTest test` from `backend/`; confirm each first failure is the expected behavior.**
- [x] **Step 3: Add Commons CSV 1.14.1 and implement the parser using RFC 4180 UTF-8 with strict decoding and no file-system writes. Implement whole-file validation without database access.**
- [x] **Step 4: Rerun targeted tests and verify no input row reaches persistence when any row fails.**
- [x] **Step 5: Commit `feat: validate curriculum csv imports`.**

### Task 3: MySQL schema and repository adapter

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__academic_catalog.sql`.
- Create: `academics/application/AcademicCatalogRepository.java`.
- Create: `academics/infrastructure/persistence/JdbcAcademicCatalogRepositoryAdapter.java`.
- Create: `academics/infrastructure/persistence/AcademicCatalogSchemaTest.java` and `AcademicCatalogRepositoryIntegrationTest.java`.

**Interfaces:**
- `AcademicCatalogRepository.createDraft(ValidatedCurriculum, actorSub) -> CurriculumSummary`; the validated value is the single source of the CSV hash.
- `AcademicCatalogRepository.findCurriculum(UUID)`, `listPublishedPrograms()`, `listPublishedCurricula(UUID)`, `listDrafts()`, and `publishDraft(UUID, actorSub)` return typed projections.
- V2 adds `academic_program`, `academic_program_revision`, `academic_subject`, `academic_subject_revision`, `academic_curriculum`, `academic_curriculum_entry`, and `academic_catalog_audit_event`; it does not alter V1 or branding audit.
- Unique constraints prevent duplicate program identity, subject code, subject revision content, curriculum version per program, and duplicate subject entry per curriculum. FK/indexes cover all reads and publication lookup.

- [x] **Step 1: Write failing H2/Flyway schema tests** for all seven tables, foreign keys, constraints and indexes.
- [x] **Step 2: Run `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd -Dtest=AcademicCatalogSchemaTest test` from `backend/`; observe missing-schema failure.**
- [x] **Step 3: Add the additive V2 migration and implement atomic draft creation, identical-revision reuse, new subject/program revisions, safe summaries and public published-only reads.**
- [x] **Step 4: Write and run repository tests for successful draft creation, duplicate-version rollback, identical revision reuse, historical immutability, draft/published visibility, audit actor/hash, missing records and concurrent publication; the composed bad-file/no-write guarantee belongs to Task 4 API tests.**
- [x] **Step 5: Commit `feat: persist versioned academic curricula`.**

### Task 4: Application use cases, secured API and internal permissions

**Files:**
- Modify: `security/ApplicationPermission.java`, rename `security/BrandingAuthoritiesConverter.java` to `security/ApplicationAuthoritiesConverter.java`, update its wiring/tests, and modify `security/SecurityConfiguration.java`.
- Create: `academics/infrastructure/web/AcademicCatalogExceptionHandler.java`, scoped to the catalog controller, rather than extending a global exception handler owned by branding.
- Create: `academics/application/AcademicCatalogQueryService.java`, `CurriculumPublicationService.java`, `academics/infrastructure/web/AcademicCatalogController.java`, DTOs and `WithAcademicCatalogPermissions.java`.
- Create: `academics/infrastructure/web/AcademicCatalogControllerTest.java` and `security/AcademicCatalogAuthoritiesTest.java`.

**Interfaces:**
- Public: `GET /api/v1/academic-catalog/programs`; `GET /api/v1/academic-catalog/programs/{programId}/curricula`.
- Admin: `POST /api/v1/admin/academic-catalog/imports` (multipart CSV), `GET /api/v1/admin/academic-catalog/drafts`, `GET /api/v1/admin/academic-catalog/curricula/{id}`, `POST /api/v1/admin/academic-catalog/curricula/{id}/publish`.
- `academic:catalog:read` and `academic:catalog:write` are separate. Provisional technical `ACADEMIC_CATALOG_VIEWER`/`ACADEMIC_CATALOG_ADMIN` claim values are not represented as UPTC roles; production mapping stays pending issuer/claim/role approval.
- Unknown admin routes/methods remain deny-by-default. Import creates a draft with 201; publish is conditional and returns conflict if the state is no longer `DRAFT`.

- [x] **Step 1: Write RED MockMvc tests** for anonymous read 200 and empty list, anonymous admin 401, reader draft list 200/import 403, admin import 201, bad CSV 400 with no database writes, missing draft 404, duplicate version 409, publish 200/audit, repeated publish 409, and unknown route/method 403.
- [x] **Step 2: Run `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd -Dtest=AcademicCatalogControllerTest,AcademicCatalogAuthoritiesTest test` from `backend/`; verify expected status mismatches before handlers/policies exist.**
- [x] **Step 3: Implement application services, DTOs, exception localization, explicit route matchers and separated permission mapping.**
- [x] **Step 4: Rerun targeted tests; verify errors do not leak CSV values and all administrative mutations are audited transactionally.**
- [x] **Step 5: Commit `feat: secure academic catalog api`.**

### Task 5: React catalog and import experience

**Files:**
- Create: `frontend/src/features/academics/contracts.ts`, `academicCatalogClient.ts`, `AcademicCatalogPage.tsx`, `AcademicCatalogPage.scss`, and API/UI tests.
- Modify: `frontend/src/App.tsx`, `frontend/src/App.scss`, `frontend/src/features/branding/contracts.ts`, and `frontend/src/features/branding/BrandingProvider` only as needed to route the implemented Programs module.

**Interfaces:**
- `AcademicCatalogClient`: `listPrograms`, `listCurricula(programId)`, `listDrafts(accessToken)`, `getCurriculum(id, accessToken)`, `importCsv(file, accessToken)`, `publishCurriculum(id, accessToken)`.
- The page renders only public published catalog data without a token; admin upload/publish controls are unavailable when no institutional access token/permission is supplied. Do not add login, demo credentials, localStorage token storage or fabricated catalog rows.
- Show the catalog as a development preview route in the local dev shell while the authoritative branding/module `available` flag remains false until institutional approval; never imply the module is production-ready because the page exists.
- Accessible states include loading, empty, populated versions/cohorts, selected file, draft review, row errors, publishing, conflict, forbidden, unauthenticated and network failure.

- [x] **Step 1: Write failing Vitest/Testing Library tests** for public empty/populated catalog, version-by-cohort rendering, safe text escaping, admin token absent, rejected MIME/size, upload success/error, row validation details, publish success/conflict/403, and keyboard-accessible navigation.
- [x] **Step 2: Run `npm test -- src/features/academics/AcademicCatalogPage.test.tsx`; verify the expected missing-page/client failures.**
- [x] **Step 3: Implement the typed client, validated response parsing, accessible page and route; expose Programs as the implemented module without enabling curricula/subjects registration prematurely.**
- [x] **Step 4: Run frontend tests, `npm run build`, and `npm run lint`; resolve regressions before proceeding.**
- [x] **Step 5: Commit frontend `feat: add academic catalog and curriculum import ui`.**

### Task 6: Architecture, operations and integrated verification

**Files:**
- Modify: `docs/architecture/c4.md`, `docs/architecture/data-model.md`, `docs/architecture/process-flows.md`, `docs/ROADMAP.md`, `docs/discovery/student-lifecycle-baseline.md`, `docs/runbook/local-development.md`, root/backend READMEs, `.env.example` if new limits are configurable.

- [x] Document the academics boundary, data ownership, version/cohort semantics, CSV template, source limitations, preview availability vs authoritative module flag, permissions and future student-enrollment integration in C4/data/process diagrams.
- [x] Run `& ..\tools\use-sdkman-java.ps1; .\mvnw.cmd verify` from `backend/` and `npm test`, `npm run build`, `npm run lint` from `frontend/`.
- [x] Run `docker compose config --quiet`; `docker compose up --build -d --wait`; verify MySQL health, Flyway V2, backend health, public empty catalog, UI route and denied anonymous import on the actual local Compose stack.
- [ ] Confirm no real student data, no secrets, clean submodule state and clean Git diff; `git diff --check` passes.
- [ ] Commit and push backend/docs and frontend repos to their existing `develop` upstreams; update the backend submodule pointer after the frontend commit.

## Execution notes

Tasks are executed sequentially in this local checkout with the test-first workflow. The parent request explicitly authorizes autonomous implementation and `develop` milestone integration; no PR, production deploy, IdP configuration or import of official records is in scope.
