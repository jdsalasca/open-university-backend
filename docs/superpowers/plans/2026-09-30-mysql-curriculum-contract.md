# MySQL Curriculum Contract and Performance Probe Plan

> **For agentic workers:** Use `superpowers:executing-plans` and `superpowers:test-driven-development` inline. Completed steps use `[x]`.

**Goal:** Keep MySQL 8.4 curriculum substring and unfiltered pagination under a local 50 ms average budget on 10,000 synthetic rows and make the check repeatable.

**Architecture:** An opt-in Spring integration test connects through dynamic datasource properties and rolls all fixtures back. A derived, immutable code/name search projection lets one covering curriculum-entry index filter and order results without 10,000 subject-table lookups. A PowerShell runner starts a fresh loopback-only MySQL container without a persistent volume, runs the test with SDKMAN Java 25 and Maven Wrapper, then removes the container.

**Tech Stack:** Java 25, Spring Boot test, Spring JDBC, Flyway, JUnit 5, MySQL 8.4, PowerShell, Docker.

**Spec:** `docs/superpowers/specs/2026-09-30-curriculum-mysql-search-performance.md`

## Global Constraints

- Keep backend and frontend separate; this milestone changes backend schema/query/tests plus shared verification docs/tooling, with no frontend source changes.
- Do not add test dependencies; the performance requirement justifies a production query/index optimization guarded by the repository's normal test suite.
- Keep `academic_subject` and `academic_subject_revision` as the normalized source of truth; search snapshots are derived and immutable.
- Backfill existing entries before making the two search fields non-null.
- Use only synthetic catalog records and roll back test fixtures.
- Bind the ephemeral database port to loopback and do not attach a persistent volume.
- Apply `<50 ms` as a local single-client synthetic regression budget; report evidence without presenting it as proof of production performance or an institutional SLO.
- Keep the long-running preview Compose database and containers untouched.

## Review Focus

- Opt-in tests must remain skipped in the normal suite unless explicitly enabled.
- The runner must fail if Docker, MySQL readiness, SDKMAN Java, or Maven verification fails.
- Contract assertions must exercise the repository query over MySQL, including accent/case insensitivity and literal `%`, `_`, and `!` search.
- Substring filtering must use the covering curriculum-entry index before joining display values for returned rows.
- The performance fixture must contain exactly 10,000 rows, return correct page/count results, and report average, p50, p95, and p99 with concurrency and sample count.
- Cleanup must remove only the uniquely named ephemeral container created by this run.

---

### Task 1: MySQL 8.4 contract and synthetic pagination probe

**Files:**
- Create: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/persistence/AcademicCatalogMySqlContractTest.java`
- Create: `backend/src/main/resources/db/migration/V3__curriculum_search_projection.sql`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/academics/infrastructure/persistence/JdbcAcademicCatalogRepositoryAdapter.java`
- Modify: `backend/src/test/java/co/edu/uptc/universiry/academics/infrastructure/persistence/AcademicCatalogRepositoryIntegrationTest.java` and `AcademicCatalogSchemaTest.java`

**Interfaces:**
- Read `UNIVERSIRY_MYSQL_TEST_URL`, `UNIVERSIRY_MYSQL_TEST_USERNAME`, and `UNIVERSIRY_MYSQL_TEST_PASSWORD` through `@DynamicPropertySource`.
- Require `-Duniversiry.mysql-contract.enabled=true`; require the independent `-Duniversiry.mysql-performance.enabled=true` property for the 10,000-entry timing probe.
- Use the existing `CurriculumPublicationService` and `AcademicCatalogQueryService` for the MySQL collation contract; seed the performance rows with bounded JDBC batches.
- Persist `search_subject_code` and `search_subject_name` from `ValidatedCurriculumEntry` in the same batch insert as each entry. Backfill those fields from `academic_subject` and `academic_subject_revision`, then enforce non-null and replace the prior order-only index with the covering search-order index.
- Place all query filters in the published curriculum entry `LEFT JOIN` for the total count; filter page rows using the search snapshots and return canonical code/name through their existing joins.

- [x] Add characterization/schema tests and a MySQL 8.4 performance budget for accent/case-insensitive search, literal `%`/`_`/`!`, and 10,000-row page/count reads with 10 warmups and 50 measured samples.
- [x] Observe RED: H2 shows the search columns/index are absent; MySQL 8.4 shows the filtered mean is 128 ms against the 50 ms budget.
- [x] Add migration/backfill and update the write/query adapter while preserving public API and literal substring semantics.
- [x] Run focused H2 catalog tests and the opt-in MySQL probe; inspect correctness, plans, and average/p50/p95/p99 output. H2: 33 tests passed. In the initial MySQL 8.4 runs, the filtered mean fell from 128 ms to 33.807 ms; later runs recorded a high search tail up to p95/p99 108.218/126.693 ms.
- [x] Keep test fixtures in the test transaction; confirm rollback and 10,000-row results.

### Task 2: Ephemeral runner and project documentation

**Files:**
- Create: `tools/verify-mysql-curriculum.ps1`
- Modify: `docs/runbook/local-development.md`, `docs/ROADMAP.md`, current pagination design, prior pagination plan, current spec and this plan.
- Create: `docs/architecture/decisions/ADR-0002-curriculum-search-snapshots.md`
- Modify: `docs/architecture/data-model.md`
- Create: `docs/superpowers/specs/2026-09-30-curriculum-mysql-search-performance.md`

- [x] Add a PowerShell runner that creates a uniquely named, loopback-only MySQL 8.4 container without a volume, waits for readiness, runs Task 1 using the SDKMAN-selected Java 25 and Maven Wrapper, and removes only that container in `finally`.
- [x] Document the verification command, local synthetic metrics and their limits in the runbook, roadmap, data model, ADR and pagination design.
- [x] Run the backend suite with `mvnw verify`, recheck Compose health and repository status, commit to `develop`, push the backend repository, and verify the remote SHA (`10e3fd7a7fb8eb25bdabde55b69adfe0c1bea46a`).

### Revalidation — 2026-09-30

- [x] Rerun `tools/verify-mysql-curriculum.ps1` against a uniquely named, temporary MySQL 8.4 container; SDKMAN selected Java 25.0.4; all 6 MySQL contract tests passed.
- [x] Record 10,000-row, concurrency-1 means/p50/p95/p99: unfiltered 11.786/11.300/12.284/29.555 ms; substring 18.205/17.996/19.781/20.891 ms; draft queue 12.698/12.628/13.447/13.552 ms. The runner removed the temporary container; the persistent Compose services remained running.
- [x] Repeat the probe three times and diagnose the intermittent tail latency. All nine averages were below 50 ms; two 50-sample nearest-rank p99 values were 170.006 ms (draft queue) and 175.024 ms (substring search), each the maximum observation in a different run. A correctly GC-instrumented repeat did not reproduce them; visible G1 pauses were approximately 4.6–7.7 ms, so the outlier cause remains unconfirmed. Full results and interpretation: [local measurement record](../specs/2026-09-30-curriculum-mysql-search-performance.md).
- [x] Extend the isolated MySQL runner with an academic-period stale-transition contract; the 20:29 run passed all 6 curriculum checks plus the period revision-preservation check and recorded a fourth set of 10,000-row latency metrics. The disposable container was removed.

### Revalidation — 2026-10-01

- [x] Rerun `tools/verify-mysql-curriculum.ps1` against a fresh disposable MySQL 8.4 container with SDKMAN Java 25.0.4-tem; all 7 MySQL contract tests passed with no failures, errors, or skips.
- [x] Record the 10,000-row, concurrency-1 measurements: draft queue 16.270/15.163/22.439/27.274 ms, unfiltered curriculum 13.218/12.850/14.812/30.554 ms, and substring-filtered curriculum 20.830/20.598/22.857/23.648 ms (average/p50/p95/p99). The runner removed the disposable container and left persistent Compose services running. Detailed evidence and limitations: [measurement record](../specs/2026-09-30-curriculum-mysql-search-performance.md).
