# Risks and controls

| Risk | Control |
| --- | --- |
| Legacy curriculum test uses a repository API that no longer exists. | Adapt the test to `findLatestPublishedCurriculum(CurriculumProgramIdentity)`; do not restore the old list-and-select contract. |
| The preview could compare against another modality, level, program, or campus. | Assert the complete identity value passed by the application service. Keep the MySQL contract test for query behavior. |
| A bulk commit could remove newer product behavior or add generated artifacts. | Compare each candidate against current `origin/develop`; transfer only unique, verified product changes. Keep screenshots, Harness/Playwright exports, logs, and stale snapshots out of commits. Preserve legacy worktrees untouched. |
| The local preview and institutional production could be conflated. | Report only current Compose container health and local URLs; make no production or institutional acceptance claim. |
| User/personal or official admissions data could enter the tests. | Use synthetic academic catalog values only; do not add personal data, admissions rules, or seed records. |
| The frontend gitlink could regress to an older local snapshot. | Do not change the frontend repository or gitlink during a backend-only documentation milestone; backend remains pinned to its current remote frontend SHA. |
