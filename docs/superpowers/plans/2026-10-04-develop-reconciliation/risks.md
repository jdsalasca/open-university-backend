# Risks and controls

| Risk | Control |
| --- | --- |
| Legacy curriculum test uses a repository API that no longer exists. | Adapt the test to `findLatestPublishedCurriculum(CurriculumProgramIdentity)`; do not restore the old list-and-select contract. |
| The preview could compare against another modality, level, program, or campus. | Assert the complete identity value passed by the application service. Keep the MySQL contract test for query behavior. |
| A bulk commit could remove newer product behavior or add generated artifacts. | Work only in a clean detached worktree and stage the three explicitly scoped paths. Preserve both legacy worktrees untouched. |
| The local preview and institutional production could be conflated. | Report only current Compose container health and local URLs; make no production or institutional acceptance claim. |
| User/personal or official admissions data could enter the tests. | Use synthetic academic catalog values only; do not add personal data, admissions rules, or seed records. |
| The frontend gitlink could regress to an older local snapshot. | Do not change the frontend repository or gitlink; backend remains pinned to its current remote frontend SHA. |
