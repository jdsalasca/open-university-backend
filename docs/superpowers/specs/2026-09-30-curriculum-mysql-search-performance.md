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

## Latest local measurement — 2026-09-30 19:44 (UTC-5)

The repeatable runner `tools/verify-mysql-curriculum.ps1` selected SDKMAN Java 25.0.4, created an isolated MySQL 8.4 container, and passed all six MySQL contract tests, including the UTC-session and cursor-publication contracts. The performance cases used 10,000-row fixtures, 10 warmups, 50 samples, page size 100 for curriculum entries and 25 for the draft queue, and concurrency 1:

| Query | Average | p50 | p95 | p99 |
|---|---:|---:|---:|---:|
| First page, no filter | 11.786 ms | 11.300 ms | 12.284 ms | 29.555 ms |
| First page, substring filter | 18.205 ms | 17.996 ms | 19.781 ms | 20.891 ms |
| Draft review queue | 12.698 ms | 12.628 ms | 13.447 ms | 13.552 ms |

All three averages and all observed p95/p99 values in this run are below 50 ms. Earlier runs measured higher search tails, including p99 values of 108.218 and 126.693 ms; the latest result does not erase that variability. `EXPLAIN ANALYZE` for the draft queue shows MySQL reading the first 26 rows directly from `ix_academic_curriculum_drafts` in reverse order before joining program metadata. The filtered catalogue page showed one matching row and only the corresponding subject/revision lookups after the indexed scan. The disposable container and synthetic, single-client workload do not establish UPTC production performance or an institutional SLA.
