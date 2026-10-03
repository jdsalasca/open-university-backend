# Directorio de servicios estudiantiles — Plan de mejora

## Goal

Make the student directory clearer and more useful by separating general Wellbeing channels from socioeconomic support and linking each card to the official UPTC page that describes it.

## Constraints

- Keep this route public and static. Do not add an API, database table, login, form, reservation, or personal data.
- Use only verified HTTPS pages under `uptc.edu.co`; state when a page's date is a verification date rather than a publication date.
- Keep text search and category filters in the browser. Keep the warning against entering credentials visible.
- Preserve mobile layout, shared SCSS tokens, secure external-link attributes, and the current Compose preview.
- Do not commit, push, merge, or change branches.

## Existing state

`StudentServicesPage.tsx` already has search, category filters, result count, empty/reset state, and six public links. The Wellbeing card currently conflates general Wellbeing with socioeconomic support and points to the general Bienestar Virtual page. The source inventory already records the distinct UPTC support page and its visible update date.

## Tasks

### 1. Correct the public service catalog

Files: `frontend/src/features/students/StudentServicesPage.tsx` and `docs/discovery/uptc-student-services-directory-2026-10.md`.

- Keep a general Wellbeing card on the Bienestar Virtual page, with only the channels that page lists.
- Add a separate socioeconomic-support card linked to the dedicated Línea de Apoyo Socioeconómico page.
- Avoid publishing eligibility, open calls, dates, cupos, or benefits as currently available; direct students to the official source for vigencia and requirements.
- Update the source inventory with the verified official URL, scope of the claim, and visible source date.

### 2. Prove the contract with AAA tests first

Files: `frontend/src/features/students/StudentServicesPage.test.tsx` and `frontend/src/App.test.tsx`.

- Assert the public directory wording accurately covers academic and wellbeing services.
- Assert the dedicated socioeconomic card links to the specific official UPTC source and exposes its source attribution/date.
- Update the mounted-route assertion to require the revised public page heading.
- Keep coverage for secure external links, no personal-data inputs, accent/case-insensitive search, combined category/text filters, result count, and empty/reset behavior.
- Run the focused test before implementation and confirm the expected failure; implement the minimum data/content change only after RED.

### 3. Verify the delivered experience

- Run the focused page test, full `npm test`, `npm run lint`, and `npm run build` with existing timeouts and bundle budgets.
- Inspect `/#estudiantes` in the running local preview at desktop and narrow viewport widths; verify the new card, filters, empty state, and privacy warning.
- Run `git diff --check`, preserve unrelated dirty changes, and record results in Harness Moon.

## Acceptance

- The directory distinguishes general Bienestar from socio-economic assistance and each card points to the relevant official UPTC HTTPS source.
- Search, combined filters, count, and reset continue to work with accents and case differences.
- No personal data, credentials, fabricated availability, or institutional selection rules are collected or inferred.
- Focused/full tests, lint, build, and local-browser review are recorded. The task remains open if any check fails.

## Execution ledger

- Preflight ruling: continue in the shared checkout because the user requested a live Compose preview and the existing route is already mounted there; reserve the exact task paths in Harness and do not edit unrelated files.
- Official sources checked on 2026-10-02 local time: Bienestar Virtual lists consejerías, mental-health and Violeta routes, emergencies, culture, human development, physical activity, sport, health, and socioeconomic support; the dedicated socioeconomic page lists restaurant, scholarships/residences, Renta Joven, and Upetecitos, updated 2025-07-20. No eligibility or current-call status will be copied into the card.
- Full-suite RED after the heading change: 427/428 passed; `App.test.tsx` still expected the old page title. Harness reserves the integration test path, and its expected heading now matches the user-visible route.
- TDD RED / GREEN: the focused page suite first reported the expected 3 failures out of 4 for the changed title/list and separate socio-economic route; after implementation, all 4/4 focused tests passed.
- Integration check: an initial combined App/page run hit the existing 5-second timeout in the space-guide route; without changing the timeout, the serial App suite passed 25/25. The final `npm test` passed 55 Vitest files / 428 tests and 17 Node checks.
- Final static validation: `npm run lint` passed with no findings. `npm run build` passed TypeScript, Vite and the existing bundle-budget verifier; entry output was 277,524 B JS / 17,715 B CSS, with the student-services chunk at 8.85 kB JS / 7.71 kB CSS.
- Runtime: `docker compose ps` confirmed frontend at `127.0.0.1:5173`, backend at `127.0.0.1:8080`, and MySQL 8.4 healthy at `127.0.0.1:3307`.
- Live behavior: the local directory exposes 7 services, all 6 category filters, source/date attribution, and the no-credentials warning. Searching `servicio sin coincidencias 9zq` announced 0 results and `Limpiar búsqueda y filtros` restored all 7.
- Responsive review: desktop screenshot at 1440 × 709 showed the sidebar, heading, privacy callout, search and category controls with no visible horizontal overflow. At 390 × 844, the accessibility snapshot retained the search, all category controls, all 7 service entries and privacy text; `documentElement.scrollWidth` equaled 390. The in-app narrow screenshot also showed the library card and footer. A second Chrome DevTools screenshot at 390 × 844 timed out, so the narrow-width visual evidence is the in-app screenshot plus the 390px accessibility/overflow check.
- Search/filter tests and live checks left the user-facing `/#estudiantes` tab at its original empty-search, all-services state and scrolled to the top. No backend, database, authentication, institutional rules, branch, commit, push or merge changes were made for this task.
