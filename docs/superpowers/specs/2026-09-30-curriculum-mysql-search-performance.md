# MySQL 8.4 curriculum search performance

**Status:** Approved implementation detail within the v0 preview; the catalog remains unpublished and contains no official rows.

## Evidence

An opt-in integration probe against an isolated MySQL 8.4 instance loaded 10,000 synthetic curriculum entries. Initial runs measured 63–64 ms for unfiltered pagination and 116–128 ms for substring search. After removing subject joins from the count, the unfiltered case fell to 19–21 ms, while substring search remained above budget.

`EXPLAIN ANALYZE` initially showed that each substring query scanned all 10,000 curriculum entries and performed up to 10,000 point lookups into both `academic_subject` and `academic_subject_revision`, once for the filtered count and again for the page query. A shared CTE was inlined/materialized separately by MySQL and did not remove the duplicate scan.

## Decision

Keep `academic_subject` and `academic_subject_revision` as the authoritative normalized records. Add two derived, immutable search snapshots—`search_subject_code` and `search_subject_name`—to each curriculum entry. Populate them atomically from the validated import row, and backfill existing entries from their referenced subject and revision in Flyway V3. They are read projections only; no API edits them independently.

Replace the narrow curriculum-entry order index with a covering index on `(curriculum_id, semester, row_order, search_subject_code, search_subject_name)`. It lets MySQL scan one curriculum's ordered index entries and evaluate substring filters from index values. The page query joins subject/revision records only for the rows returned to the caller. Both filtered and unfiltered counts use only the curriculum-entry index plus the published-curriculum gate.

Search remains a literal substring query over code or name, case/accent-insensitive under the configured MySQL collation. `%`, `_`, and `!` remain escaped as literal characters. The source values in the normalized records remain the only editable truth; the extra values and wider index are a documented read-optimization tradeoff.

## Acceptance

- Flyway migrates both empty and pre-existing catalog entries and leaves the search snapshots non-null.
- Imports persist each snapshot from the validated row; public search results, wildcard escaping, paging, ordering, and published-only behavior remain unchanged.
- No new runtime dependency, API response field, or frontend behavior is introduced.
- The opt-in MySQL 8.4 probe inserts exactly 10,000 synthetic entries, validates totals and page bounds, and reports average, p50, p95, and p99 for no-filter and substring-filter reads.
- The observed mean of each scenario is below 50 ms on the local single-concurrency synthetic setup. This is a local regression budget, not a production SLA certification.
- The normal test suite does not require Docker or run the performance probe unless its explicit system properties are enabled.

## Repeated local measurements — 2026-09-30 20:11–20:29 (UTC-5)

Four invocations of `tools/verify-mysql-curriculum.ps1` selected SDKMAN Java 25.0.4 and created unique isolated MySQL 8.4 containers. The first three passed all six curriculum contracts; the fourth used the updated runner and passed those six plus the new stale-period-transition contract (seven tests total). Every performance case used 10,000-row fixtures, 10 warmups, 50 measured samples, page size 100 for curriculum entries and 25 for the draft queue, and concurrency 1. The third invocation enabled valid JVM GC logging.

| Run (UTC-5) | Query | Average | p50 | p95 | p99 |
|---|---|---:|---:|---:|---:|
| 20:11 | First page, no filter | 11.026 ms | 10.728 ms | 11.615 ms | 20.755 ms |
| 20:11 | First page, substring filter | 18.109 ms | 18.021 ms | 18.952 ms | 20.625 ms |
| 20:11 | Draft review queue | 19.273 ms | 14.355 ms | 32.144 ms | 170.006 ms |
| 20:13 | First page, no filter | 12.540 ms | 12.424 ms | 13.839 ms | 13.959 ms |
| 20:13 | First page, substring filter | 25.258 ms | 20.238 ms | 33.792 ms | 175.024 ms |
| 20:13 | Draft review queue | 13.402 ms | 13.261 ms | 14.767 ms | 19.066 ms |
| 20:16, GC logging | First page, no filter | 11.382 ms | 11.217 ms | 13.555 ms | 14.814 ms |
| 20:16, GC logging | First page, substring filter | 18.606 ms | 18.300 ms | 21.680 ms | 23.386 ms |
| 20:16, GC logging | Draft review queue | 13.064 ms | 12.807 ms | 15.056 ms | 17.291 ms |
| 20:29 | First page, no filter | 10.964 ms | 10.846 ms | 11.969 ms | 13.822 ms |
| 20:29 | First page, substring filter | 18.350 ms | 18.088 ms | 19.758 ms | 28.711 ms |
| 20:29 | Draft review queue | 13.200 ms | 12.888 ms | 14.732 ms | 22.810 ms |

All twelve means were below the local `<50 ms` average budget (10.964–25.258 ms). The two new tail outliers appeared in different queries and did not recur in either the GC-logged run or the 20:29 rerun. With 50 samples, the nearest-rank p99 is the maximum sample (`ceil(0.99 × 50) = 50`), so an isolated pause determines this reported percentile. In the diagnostic run, observed G1 pauses were approximately 4.6–7.7 ms and no comparable pause appeared in the GC log. The cause of the isolated latency outliers is not established; this evidence does not justify attributing them to SQL, garbage collection, or a particular host event. Earlier runs also observed search p95/p99 up to 108.218/126.693 ms, preserving evidence of variability.

`EXPLAIN ANALYZE` for the draft queue shows MySQL reading the first 26 rows directly from `ix_academic_curriculum_drafts` in reverse order before joining program metadata. The filtered catalogue page showed only the corresponding subject/revision lookups after its indexed scan. The disposable container and synthetic, single-client workload do not establish UPTC production performance or an institutional SLA; representative hardware, concurrency, workload mix, and acceptance thresholds remain to be agreed with institutional owners.

## Latest local Compose MySQL run — 2026-09-30 (UTC-5)

The opt-in MySQL contract and performance classes also passed against the MySQL 8.4 database already used by the local Compose preview. Java in the backend container was 25.0.4.1 and Flyway schema version 11 was current. Seven MySQL-backed contract tests passed with no failures or skips. The performance fixture used 10,000 synthetic rows per scenario, 10 warmups, 50 samples, page size 100 (25 for the draft queue), and concurrency 1. Fixture writes were transactional and rolled back after each test.

| Query | Average | p50 | p95 | p99 |
|---|---:|---:|---:|---:|
| Draft review queue, first page | 11.259 ms | 11.193 ms | 13.145 ms | 13.740 ms |
| Curriculum first page, no filter | 9.746 ms | 9.616 ms | 11.702 ms | 15.916 ms |
| Curriculum substring-filter page | 16.759 ms | 16.741 ms | 18.293 ms | 18.809 ms |

All three latest means were below the local `<50 ms` average regression budget. This run reuses the Compose development database rather than the disposable container used by the four prior runs; the synthetic fixtures rolled back, and follow-up public catalog, structure, period, and health reads remained empty/healthy. As before, the result is local single-concurrency evidence only, not a production SLO or representative institutional workload.

## Disposable MySQL 8.4 revalidation — 2026-10-01 (UTC-5)

`tools/verify-mysql-curriculum.ps1` ran with SDKMAN Java 25.0.4-tem against a fresh MySQL 8.4 container bound to an ephemeral loopback port, with no persistent volume. The two MySQL contract classes passed all seven tests with no failures, errors, or skips. Each performance scenario used 10,000 synthetic rows, 10 warmups, 50 measured samples, and concurrency 1; page size was 100 for curriculum reads and 25 for the draft queue.

| Query | Average | p50 | p95 | p99 |
|---|---:|---:|---:|---:|
| Draft review queue, first page | 16.270 ms | 15.163 ms | 22.439 ms | 27.274 ms |
| Curriculum first page, no filter | 13.218 ms | 12.850 ms | 14.812 ms | 30.554 ms |
| Curriculum substring-filter page | 20.830 ms | 20.598 ms | 22.857 ms | 23.648 ms |

All three means were below the local `<50 ms` average regression budget. The runner's `finally` block removed the disposable container; the persistent Compose database and applications stayed running. This is a single run on the development machine, using synthetic fixtures and concurrency 1. It does not establish production performance or an institutional SLA; representative hardware, concurrency, workload mix, and acceptance thresholds remain to be agreed with institutional owners.

## Second disposable MySQL 8.4 revalidation — 2026-10-01 05:47 (UTC-5)

`tools/verify-mysql-curriculum.ps1` selected SDKMAN Java 25.0.4-tem and ran against a fresh MySQL 8.4 container without a persistent volume. All seven MySQL-backed contract tests passed with no failures, errors, or skips. Performance scenarios used 10,000 synthetic rows, 10 warmups, 50 measured samples, and concurrency 1; page size was 100 for curriculum reads and 25 for the draft queue.

| Query | Average | p50 | p95 | p99 nearest-rank |
|---|---:|---:|---:|---:|
| Draft review queue, first page | 12.789 ms | 12.682 ms | 13.900 ms | 14.099 ms |
| Curriculum first page, no filter | 11.149 ms | 11.130 ms | 12.050 ms | 13.161 ms |
| Curriculum substring-filter page | 18.432 ms | 18.005 ms | 21.597 ms | 21.951 ms |

All three means remain below the local `<50 ms` development regression budget. The disposable container was removed and the persistent Compose database and applications remained available. These are repository-query measurements on synthetic fixtures with concurrency 1, not representative production performance or an institutional SLA.
