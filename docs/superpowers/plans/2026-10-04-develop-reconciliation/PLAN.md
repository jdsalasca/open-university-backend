# Plan: reconcile local work and strengthen curriculum preview coverage

## Goal

Keep both remote repositories on `develop`, avoid overwriting newer work with stale local copies, and add service-level regression coverage for the approved read-only curriculum comparison preview.

## Baseline at plan start

- Backend remote `develop`: `421f7b2d6993ae4ffdf282ef54189e2566764c7d`.
- Frontend remote `develop`: `95f6a0404fe6d03c216b252d3c795b6679b71a5a`.
- `git ls-remote --heads origin` returns only `develop` for each repository.
- The active checkouts are clean and the Docker Compose preview is running at `http://localhost:5175`; backend is mapped to `127.0.0.1:8080` and MySQL reports healthy.
- The legacy backend checkout is on `agent/coordination-espejo`, 84 commits behind and 1 ahead, with 87 changed paths. Its one unique commit is a multi-agent coordination mirror, which conflicts with the current instruction not to create subagents.
- The legacy frontend checkout is on `develop`, 52 commits behind and 0 ahead, with 41 changed paths.
- The curriculum comparison and mobile navigation work are already represented in current remote code and tests. The old comparison commit points at an older frontend gitlink. The library barcode documentation commit is already an ancestor of backend `develop`.
- The legacy checkouts also contain generated Harness/Playwright artifacts and stale snapshots. Preserve them; do not stage or rewrite them as a batch.

## Scope

1. Add AAA service tests proving the preview asks for the latest published curriculum using all four identity dimensions: program code, academic level, study modality, and campus code.
2. Add an AAA case for a valid preview when no published reference exists. The response must report `NO_REFERENCE` and must not invent counts or samples.
3. Record the reconciliation decision and current checkout evidence in the coordination mirror.
4. Commit the plan, tests, and coordination update from this detached worktree based on `origin/develop`; push only a fast-forward to `origin/develop` and verify the remote SHA and CI.

## Out of scope

- Bulk-committing generated artifacts, screenshots, Harness exports, stale snapshots, or unchanged copies; deleting or cleaning legacy checkouts.
- Cherry-picking stale feature branches without comparing them with current `develop`, or reverting newer remote code.
- Changes to admissions, student records, identities, grades, offers, enrollment, official master data, schemas, or institutional operating rules.
- Performance claims or production readiness claims.

## Acceptance criteria

- Tests use synthetic curriculum data and AAA structure.
- A changed program, level, modality, or campus identity cannot silently receive a comparison for another published curriculum.
- An absent reference yields `NO_REFERENCE` with null comparison counts and no samples.
- The targeted Maven test and repository whitespace check pass.
- Only backend `develop` receives the documentation/test commit; frontend remains at its existing verified `develop` SHA.
- Both remote repositories still expose only `develop`; the remote backend SHA is verified after push.
- Legacy local content remains intact and is explicitly left for individual review.

## Integration plan

Use the existing detached worktree after moving it to the latest `origin/develop`. Stage only this plan directory, the new application test, and `docs/coordination/active-tasks.md`. Run the targeted backend test, inspect the staged diff, commit with an imperative message, push `HEAD:develop` only if it is a fast-forward, then fetch and verify the SHA and CI. Do not publish or delete any other local branch.

## Execution record

- `2026-10-04`: Java 25 targeted test `DefaultCurriculumPublicationServiceTest` passed (2 tests, 0 failures). Commit `3482885ed292c3a7bf387e38be499440f08a6763` is present in local and remote backend `develop`; CI `37184155245` passed the full Maven suite and MySQL contract tests.
- Follow-up documentation commit `7011319f29cf3b13a0e6f5de4f42d5914fd84129` is present in backend `origin/develop`; CI `37184484129` passed. Frontend `develop` is `95f6a0404fe6d03c216b252d3c795b6679b71a5a`, with CI `37181629590` passed. Both active repositories expose only `develop`; Compose frontend, backend health and MySQL were verified in the prior checkpoint.
- `2026-10-04` re-audit after the user's develop-only integration request: both active worktrees are clean and match their remote `develop` SHAs; `git ls-remote --heads origin` returns only `develop` for both repositories. Checked the stale backend branch (`agent/coordination-espejo`, 86 commits behind and one unique coordination-only commit), stale frontend checkout (52 commits behind), and the existing clean worktrees for curriculum comparison, catalogue, and delivery. Current `develop` already has newer versions of the curriculum comparison, public catalogue, student-services directory, and unified home; no missing product delta was identified for transfer. The coordination-only commit and generated local artifacts remain untouched. No legacy worktree or branch was deleted or rewritten.
- Harness accepted the plan but kept the active task at `planning/write_plan`; its TDD evidence endpoint rejected the test result because the task phase had not advanced. A checkpoint records the verified test, push, CI, and next reconciliation action.
