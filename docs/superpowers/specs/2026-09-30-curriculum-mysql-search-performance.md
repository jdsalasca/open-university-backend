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

## Final local measurement — 2026-09-30

The repeatable runner `tools/verify-mysql-curriculum.ps1` passed all three MySQL contract tests on the same 10,000-row fixture, with 10 warmups, 50 samples, page size 100 and concurrency 1:

| Query | Average | p50 | p95 | p99 |
|---|---:|---:|---:|---:|
| First page, no filter | 22.062 ms | 22.010 ms | 24.060 ms | 25.420 ms |
| First page, substring filter | 33.807 ms | 33.544 ms | 37.983 ms | 40.694 ms |

Both averages pass the local `<50 ms` regression gate. One earlier repeated run observed a substring p99 of 52.894 ms; the test gates the requested mean, while percentiles remain diagnostic. `EXPLAIN ANALYZE` on the filtered page showed one matching row in this scenario and only the corresponding subject/revision lookups after the indexed scan. The disposable container and synthetic, single-client workload do not establish UPTC production performance or an institutional SLA.
