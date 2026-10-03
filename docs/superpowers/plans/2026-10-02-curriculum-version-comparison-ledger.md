# Execution ledger: curriculum version comparison

Date: 2026-10-03
Plan: [implementation plan](2026-10-02-curriculum-version-comparison.md)

## Delivered behavior

- The CSV preview compares normalized subject codes against the most recently published curriculum with the exact same program, level, modality and campus identity.
- Equal publication timestamps resolve by ascending curriculum UUID. Drafts and other identities cannot be references.
- The comparison reports full added, removed, modified and unchanged counts, with at most ten examples per category. Credit values compare numerically, independent of decimal scale.
- With no reference, the response reports `NO_REFERENCE` and no fabricated zero counts. A frontend connected to an older backend omits the panel.
- Preview remains read-only. No schema, seed, audit, draft-creation, authorization or official curriculum-data behavior was added.
- C4 boundaries remain unchanged. The process flow and data-model notes describe the new derived read-only behavior.

## Verification evidence

| Check | Result |
|---|---|
| `npm exec vitest -- run src/features/academics/curriculumVersionComparisonContract.test.ts src/features/academics/AcademicCatalogPage.test.tsx` | 40 tests passed |
| `npm test` in `frontend` | 61 files, 430 Vitest tests and 15 Node checks passed |
| `npm run lint` in `frontend` | passed |
| `npm run build` in `frontend` | passed; entry JS 281,577 B; `#programas` JS 330,632 B against 331,500 B cap; comparison panel is a separate lazy chunk |
| `.\mvnw.cmd test` in `backend` on Java 25.0.4 | 401 tests, 0 failures/errors, 13 tests skipped; build passed |
| `.\mvnw.cmd -Dtest=AcademicCatalogMySqlContractTest -Duniversiry.mysql-contract.enabled=true test` | 8 MySQL contract tests passed, 2 performance-only cases skipped against a separate temporary MySQL 8.4 container |
| Compose frontend `http://localhost:5175/` | HTTP 200 |
| Compose backend `/actuator/health` | `UP` |
| Temporary contract database | stopped and removed; port 3317 verified closed; preview Compose database was not modified |

## Coordination and integration

- Harness project `project_2ac92f98-5605-4135-8c27-cee6d7144655`; requested task `task_be561bb9-9654-4554-b8fd-bf4382f0eea4`.
- The Harness agent is assigned to a different task, and the curriculum task remains in `planning`; a claim was rejected because of the existing assignment, and `validation` evidence was rejected as out of phase. The unrelated Library task was left untouched. Harness has the plan evidence; implementation and validation continue independently until the task's ownership/phase is reconciled.
- The current user-provided `AGENTS.md` forbids branch switching, merge and push without fresh approval. Work remains on its existing isolated feature branches. Local commits can be created and recorded; remote integration remains pending.
- No official curriculum records, personal data, institutional rule changes or production cutover were used.
