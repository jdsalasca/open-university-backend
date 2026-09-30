# Curriculum Server Pagination Implementation Plan

> **For agentic workers:** Use `superpowers:executing-plans` inline and `superpowers:test-driven-development`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver published curriculum subjects in filtered, stable server-side pages capped at 100 rows.

**Architecture:** The Spring academics module owns query validation, published-only filtering, counts and MySQL pagination. The React page requests one bounded page at a time and retains an accessible search/semester/paging experience. The administrative draft review contract remains unchanged.

**Tech Stack:** Java 25 via SDKMAN 25.0.4-tem, Spring Boot 4.1.1, Spring JDBC, Flyway, MySQL 8.4, React 19, TypeScript, Vite, SCSS, Vitest, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-30-curriculum-server-pagination-design.md`

## Global Constraints

- Backend and frontend remain separate `develop` repositories; the frontend stays attached as the backend repository's `frontend/` submodule.
- Use SDKMAN Java 25.0.4-tem and Maven Wrapper for host backend verification.
- Use TDD: write AAA behavior tests, observe the expected RED, implement the minimum, and verify GREEN before refactoring.
- Public reads expose only `PUBLISHED` curricula; draft review remains behind the existing read permission and retains full detail for publication review.
- Public page size defaults to 100 and accepts only 1–100 rows; page numbers are 1-based.
- Escape SQL wildcard input and bind every parameter; no SQL assembled from user search text.
- Keep the existing schema/index unless query evidence shows a required migration; never edit applied migrations.
- Do not add institutional data, student PII, fake public catalogue rows, or production OIDC mappings.
- The average MySQL query objective `<50 ms` remains unverified without an approved representative dataset and workload; report percentiles if measured.

## Review Focus

- `page`, `pageSize`, search length and semester at, below and above their bounds must return stable 400 responses before an expensive read.
- A missing or `DRAFT` curriculum must be indistinguishable on both metadata and page endpoints; neither rows nor counts may leak.
- `%`, `_` and the chosen escape character in user search must match literally, while MySQL's accent/case-insensitive collation preserves the existing search experience.
- Ordering and offsets must remain deterministic for rows sharing a semester; page boundaries must neither duplicate nor omit entries.
- Rapid search/filter/page changes must cancel stale requests, reset to page 1 when filters change and render at most 100 rows.

---

### Task 1: Published-only paged backend query and API

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/application/CurriculumEntriesPageQuery.java` and `AcademicCurriculumEntriesPage.java`.
- Create: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/web/AcademicCurriculumEntriesPageResponse.java`.
- Modify: `academics/application/AcademicCatalogRepository.java`, `AcademicCatalogQueryService.java`, `DefaultAcademicCatalogQueryService.java`, `academics/infrastructure/persistence/JdbcAcademicCatalogRepositoryAdapter.java`, `academics/infrastructure/web/AcademicCatalogController.java`, `AcademicCatalogExceptionHandler.java`, `security/SecurityConfiguration.java`, `messages.properties`, `messages_en.properties`, and the public curriculum response mapping.
- Test: `academics/infrastructure/web/AcademicCatalogControllerTest.java` (full HTTP-to-H2 persistence path; H2 does not certify MySQL's accent-insensitive collation).

**Interfaces:**
- `CurriculumEntriesPageQuery(page, pageSize, search, semester)` is an application value validated for page 1–2,147,483,647, pageSize 1–100, trimmed search ≤ 120 Unicode code points, and optional semester 1–32767. Invalid values raise a typed application exception mapped to a localized 400.
- `AcademicCurriculumEntriesPage` contains `curriculumId`, page, pageSize, totalItems, totalPages and immutable `List<AcademicCurriculumEntrySummary>`; page results use `Optional` so absent/draft differs from a published curriculum with zero matching entries.
- `AcademicCatalogRepository.findPublishedCurriculumSummary(UUID)` returns `Optional<CurriculumSummary>`; `findPublishedCurriculumEntries(UUID, CurriculumEntriesPageQuery)` returns `Optional<AcademicCurriculumEntriesPage>` with filtered count and limited ordered rows. Preserve `findCurriculum(UUID)` for full authorized draft review.
- `GET /api/v1/academic-catalog/curricula/{id}` returns the published `AcademicCurriculumResponse` as the root JSON object, without `entries`; `GET /api/v1/academic-catalog/curricula/{id}/entries?page=1&pageSize=100&search=&semester=` returns the page response. Missing and draft records return the existing safe 404 contract.
- SQL filters published status before both count and page reads, orders by `(semester, row_order)`, uses bound parameters, and escapes `%`, `_`, and `!` for literal `LIKE` search. Count and rows share a read-only transaction; calculate offset as `long`.

- [x] **Step 1: Write controller integration RED tests** named `published_curriculum_metadata_excludes_entry_rows`, `published_curriculum_entries_are_returned_in_stable_bounded_pages`, `public_entry_search_and_semester_filters_return_filtered_totals`, `entry_search_treats_sql_wildcards_literally`, `public_curriculum_entries_hide_missing_and_drafts`, `rejects_invalid_published_entry_page_parameters`, and `returns_zero_pages_and_empty_entries_for_no_matches`. Seed only through the existing CSV import/publication flow; assert first/intermediate/final pages, defaults, counts, filters, ordering, all input bounds, localized error bodies, and 404 privacy.
- [x] **Step 2: Run the focused backend tests** from `backend/` in one PowerShell process:

```powershell
& ..\tools\use-sdkman-java.ps1
& .\mvnw.cmd -Dtest=AcademicCatalogControllerTest test
```

Expected: behavior assertions fail because metadata still wraps all rows, and anonymous page requests hit the existing default-deny route policy with 401; no compilation/setup errors.
- [x] **Step 3: Add the application query/page values and repository interface methods**, keeping framework types out of the application contract and reusing the existing entry-summary type.
- [x] **Step 4: Implement query validation, localized error mapping, response mapping and the `/entries` route; add only the new GET route to the explicit anonymous security allowlist; add the published-only count and limited row SQL** in `JdbcAcademicCatalogRepositoryAdapter`. Reuse `ix_academic_curriculum_entry_order_lookup`, bind all user values, escape literal wildcards, and keep writes/default-deny and administrative detail unchanged.
- [x] **Step 5: Rerun focused controller integration tests**; expected: new page/filter tests pass and existing catalog regressions remain green.
- [x] **Step 6: Run focused controller/security tests and the complete backend suite** using SDKMAN Java 25.0.4-tem. Expected: all catalog, identity, branding and security tests pass.
- [x] **Step 7: Commit backend code** as `feat: page published curriculum entries`.

### Task 2: React server-paged curriculum detail

**Files:**
- Modify: `frontend/src/features/academics/contracts.ts`, `academicCatalogClient.ts`, `AcademicCatalogPage.tsx`, `AcademicCatalogPage.scss`, `AcademicCatalogPage.test.tsx`, and `academicCatalogClient.test.ts`.

**Interfaces:**
- Add `AcademicCurriculumEntriesPage` and a typed client call `listPublishedCurriculumEntries(id, { page, pageSize, search, semester }, signal)`; runtime parsing rejects malformed IDs, counts, entries, out-of-range values and oversized page payloads. Change `getPublishedCurriculum(id)` to parse the top-level metadata object as `AcademicCurriculum`.
- Public details request metadata and the first entries page in parallel, validate metadata against the selected curriculum card, then use server pages for subsequent filters and navigation; authorized draft review continues to use `AcademicCurriculumDetails` and its existing client method.
- Search is debounced 250 ms; page/filter requests use `AbortSignal`. Changing search or semester resets to page 1. Rendered entry rows never exceed the server page.

- [x] **Step 1: Write client RED tests** named `parses_public_curriculum_metadata_without_entries`, `requests_public_curriculum_entries_with_encoded_filters`, `parses_bounded_entry_pages`, and `rejects_malformed_or_oversized_page_responses`; assert parameters are encoded and bearer credentials are omitted.
- [x] **Step 2: Run the focused client tests** with `npm.cmd test -- src/features/academics/academicCatalogClient.test.ts` from `frontend/`. Expected: absent page client/parser assertions fail, without import or compilation errors. RED: 17 tests, 9 expected failures, 0 errors.
- [x] **Step 3: Implement page contract parsing and URLSearchParams query construction**; do not store credentials or include cookies.
- [x] **Step 4: Write UI RED tests** named `requests_only_the_first_bounded_page`, `loads_the_next_server_page`, `debounces_search_and_resets_pagination`, `filters_by_numeric_semester`, `shows_no_server_matches`, and `rejects_entries_for_another_curriculum`. Assert a maximum of 100 rows and that draft/admin review behavior remains unchanged.
- [x] **Step 5: Run the focused UI tests** with `npm.cmd test -- src/features/academics/AcademicCatalogPage.test.tsx`. RED observed: 24 tests, 6 expected UI failures, 0 errors before the public detail component changed.
- [x] **Step 6: Implement the bounded table, server query effects, cancellable debounce, numeric semester filter and accessible page controls**; remove local slicing of a full curriculum response.
- [x] **Step 7: Run the complete frontend suite, production build, lint and `git diff --check`**; expected: all pass, including catalog, branding and navigation regressions. GREEN: 7 files/68 tests; production build and lint pass.
- [x] **Step 8: Commit frontend code** as `feat: browse curriculum entries by server page` (`ca2841c`, pushed to `origin/develop`). Frontend contract documentation followed in `4ecfda3`, also pushed.

### Task 3: Documents and integrated milestone

**Files:**
- Modify: `docs/architecture/data-model.md`, `docs/architecture/process-flows.md`, `docs/architecture/c4.md`, `docs/ROADMAP.md`, `docs/superpowers/specs/2026-09-29-academic-catalog-design.md`, root/backend/frontend `README.md`, and the `frontend` submodule gitlink.

- [x] Document that public details return metadata and public entry reads use bounded server pagination/search; show the count/page SQL path, transaction and published-only gate. Keep the administrative draft review flow separate. Note that H2 cannot certify MySQL accent-insensitive collation.
- [x] Update roadmap v0 delivery status without marking the official catalogue active or H4/student lifecycle complete.
- [x] Run `docker compose config --quiet`; confirm Compose services remain healthy, `/actuator/health` is UP, public program list remains `[]`, frontend returns 200, and an unknown curriculum's metadata and entries both return 404. Run read-only `LOWER(...) LIKE ... ESCAPE '!'` queries against Compose MySQL with case/accent and wildcard examples under the observed local collation; no database rows are added.
- [x] Confirm the Compose Watch process remains active and Vite HMR sees the frontend change. Do not seed official or persistent fake public rows.
- [x] Re-run `npm audit --json`; report its advisory count as a date-stamped frontend check, not a backend Java SCA result.
- [ ] Commit backend documentation and the frontend submodule pointer as `docs: describe server-paged curriculum reads`; push both repositories' `develop` branches and verify remote SHAs and clean worktrees.

## Execution note

The user authorized continuous autonomous work across both repositories, TDD, and `develop` milestone pushes. The v0 response-shape replacement is limited to the unpublished local preview; production UPTC deployment, official records, SSO role mapping, student lifecycle rules and the `<50 ms` performance claim remain outside what this plan can authorize or prove.
