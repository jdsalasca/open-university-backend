# Access Role Assignments Implementation Plan

> **For agentic workers:** Use `superpowers:executing-plans` to implement this plan task-by-task. Keep the TDD RED-GREEN-REFACTOR cycle for every behavior.

**Goal:** Provide a usable, audited identity and role-assignment foundation with typed university scopes, while keeping institutional access closed until UPTC configures its identity provider and bootstrap authority.

**Architecture:** OIDC remains the only authentication mechanism. The backend registers only the authenticated `issuer+subject`, stores assignments and append-only audit records in MySQL, and calculates the effective permissions returned by `/api/v1/me`. A fixed role catalog contains the eight requested profiles. `ADMINISTRATOR` requires exactly `UNIVERSITY` scope and grants only access-management permissions; it does not automatically inherit every platform permission. Other profiles remain permission-neutral until their domain authorization matrix is approved. The React console uses protected APIs and displays opaque identity subjects without copying email or other profile data.

**Tech Stack:** Java 25.0.4 (`.sdkmanrc`), Spring Boot 4.1.1, Spring Security Resource Server, JDBC, Flyway, MySQL 8.4, React 19, TypeScript, Vite, SCSS, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-01-identity-and-scoped-access-proposal.md`

## Global Constraints

- Keep role management and every mutation denied by default; the OIDC issuer, audience, claim mapping and initial authority remain empty unless institutionally configured.
- Never create a local password, demo identity, seeded administrator, email directory, or personal profile field.
- Store the authenticated identity only as the validated opaque pair `issuer+subject`.
- Support the requested roles `APPLICANT`, `ADMITTED`, `STUDENT`, `TEACHER`, `ADMINISTRATIVE`, `ADMISSIONS`, `DIRECTIVE`, and `ADMINISTRATOR`.
- `APPLICANT`, `ADMITTED`, and `STUDENT` memberships are derived from a verified lifecycle source and cannot be manually assigned in this increment.
- A role assignment has typed scopes; scopes within one assignment intersect (AND), multiple valid assignments combine (OR), and global administration requires the explicit `UNIVERSITY` scope.
- Every role grant or revocation carries actor, date, version, institutional reference, and a separate append-only access audit event in one transaction.
- Enforce permissions in Spring Security and the application service; frontend visibility is not authorization.
- Keep existing claim-to-permission mapping functional and empty by default. Role assignments add only permissions explicitly listed in the immutable profile catalog; no role name grants permissions by convention.
- Preserve the React/Java monolith split, existing interfaces, Flyway ownership, Compose Watch, and Spanish-first backend i18n.
- Do not change the <50 ms performance goal or claim it has been measured.

## Review Focus

- Missing, blank, oversized, or malformed OIDC issuer/subject must fail closed and must not create an identity row.
- A subject with the same text under another issuer is a different identity and must not inherit assignments.
- A profile assignment with no scope, invalid scope reference, invalid date interval, or blank source reference must be rejected without an audit row.
- Self-assignment, self-revocation, unknown profiles, and manual applicant/admitted/student assignment must be denied.
- Revocation with a stale version, repeated revocation, or a partial transaction must not grant stale access or leave an audit-only event.

---

### Task 1: Identity and role domain contracts

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/AuthenticatedPrincipal.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/ScopeKind.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/RoleProfile.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/AssignmentScope.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/ResourceDescriptor.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/AssignmentStatus.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/InstitutionalReference.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/RoleAssignment.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/AccessAuditAction.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/domain/AccessAuditEvent.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/ApplicationPermission.java`
- Create tests in `backend/src/test/java/co/edu/uptc/universiry/identity/domain/`.

**Interfaces:**
- `AuthenticatedPrincipal(String issuer, String subject)` validates both bounded opaque values and compares both exactly.
- `RoleProfile` exposes `key()`, `displayName()`, `manuallyAssignable()`, `allowedScopeKinds()`, and `permissions()` from one immutable allowlisted catalog. Only `ADMINISTRATOR` currently adds `identity:roles:read/write`; all other profiles remain permission-neutral until their action matrices are approved.
- `AssignmentScope(ScopeKind kind, String stableReference)` represents `UNIVERSITY`, `SITE`, `FACULTY`, `PROGRAM`, or `JOB_APPOINTMENT`; `UNIVERSITY` has no reference and all other kinds require one.
- `RoleAssignment` contains UUID, target principal, profile, scopes, inclusive validity, status, grantor principal, typed institutional reference, created time, and optimistic-lock version.
- `ResourceDescriptor` contains normalized references for the resource's applicable site, faculty, program, and appointment scopes; it does not resolve institutional hierarchy by labels.
- `AccessAuditEvent` contains the actor, institutional reference, event time, assignment identity, action, and a single monotonic version transition.
- `RoleAssignment.matches(ResourceDescriptor, LocalDate)` requires every scope in one assignment to match; union across assignments is handled by the policy service.

- [ ] **Step 1: Write AAA tests** for exact issuer/subject identity, eight allowlisted profiles, nonassignable lifecycle profiles, required university scope for administrator, typed scope validation, AND matching, expired assignment, and OR across separate assignments.
- [ ] **Step 2: Run tests and confirm RED** because the domain types and rules do not exist.
- [ ] **Step 3: Implement the smallest immutable records/enums and validation rules**; do not add an editable permission catalogue.
- [ ] **Step 4: Run the domain tests and confirm GREEN**, then run all backend tests.
- [ ] **Step 5: Commit** the domain contracts and tests.

### Task 2: MySQL persistence and atomic audit

**Files:**
- Create: `backend/src/main/resources/db/migration/V17__identity_role_assignments.sql`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/application/IdentityDirectory.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/application/RoleAssignmentRepository.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/infrastructure/persistence/JdbcIdentityDirectoryAdapter.java`
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/infrastructure/persistence/JdbcRoleAssignmentRepositoryAdapter.java`
- Create schema and adapter tests in `backend/src/test/java/co/edu/uptc/universiry/identity/infrastructure/persistence/`.

**Interfaces:**
- `IdentityDirectory.registerIfAbsent(AuthenticatedPrincipal, Instant)` stores only issuer, subject, and first-seen timestamp.
- `RoleAssignmentRepository.findIdentitiesBySubjectPrefix(String, int)`, `findAssignments(AuthenticatedPrincipal, LocalDate)`, `create(RoleAssignment, AccessAuditEvent)`, and `revoke(UUID, long expectedVersion, AccessAuditEvent)` provide the application port.
- The schema uses foreign keys to `academic_site`, `academic_organization_unit`, and `academic_program`; `JOB_APPOINTMENT` stores only an opaque external stable identifier because no local HR master is approved.

- [ ] **Step 1: Write schema/adapter AAA tests** for identity uniqueness by `(issuer, subject)`, scope foreign keys, exact eight-role check constraint, no seeded identity/assignment, append-only audit, optimistic version conflict, and assignment-plus-audit rollback.
- [ ] **Step 2: Run tests and confirm RED** because migration and adapters are absent.
- [ ] **Step 3: Add migration V17 and JDBC adapters** using prepared statements and transaction boundaries; no SQL access outside the repository adapter.
- [ ] **Step 4: Run adapter/schema tests on H2 and the MySQL 8.4 contract suite**, confirm GREEN, then run all backend tests.
- [ ] **Step 5: Commit** the schema, ports, adapters, and tests.

### Task 3: Assignment use cases, `/api/v1/me`, and protected API

**Files:**
- Create: `backend/src/main/java/co/edu/uptc/universiry/identity/application/RoleAssignmentService.java`
- Create request/response records and `RoleAssignmentController` under `identity/infrastructure/web/`.
- Modify: `backend/src/main/java/co/edu/uptc/universiry/identity/infrastructure/web/CurrentIdentityController.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/identity/infrastructure/web/CurrentIdentityResponse.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/ApplicationPermission.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/ApplicationAuthoritiesConverter.java`
- Modify: `backend/src/main/java/co/edu/uptc/universiry/security/SecurityConfiguration.java`
- Modify: `backend/src/main/resources/messages.properties` and `messages_en.properties`.
- Create tests in `backend/src/test/java/co/edu/uptc/universiry/identity/application/` and `identity/infrastructure/web/`; extend security tests.

**Interfaces:**
- `RoleAssignmentService.assign(AuthenticatedPrincipal actor, CreateRoleAssignmentCommand)` and `revoke(AuthenticatedPrincipal actor, UUID assignmentId, long expectedVersion, String sourceReference)` enforce non-self-service administration, profile assignment rules, scopes, source reference, interval, and audit.
- `GET /api/v1/admin/access/role-profiles`, `GET /api/v1/admin/access/identities?subjectPrefix=&limit=`, `GET /api/v1/admin/access/assignments?issuer=&subject=`, `POST /api/v1/admin/access/assignments`, and `PATCH /api/v1/admin/access/assignments/{id}/revoke` are protected by `identity:roles:read/write`.
- A valid `GET /api/v1/me` registers the authenticated pair and returns known permissions plus the caller's current role/scope assignments with `Cache-Control: no-store`; it never returns email or other token claims.
- `ADMINISTRATOR` requires exactly `UNIVERSITY` scope and grants only `identity:roles:read` and `identity:roles:write`; it does not grant academic, branding, or future module permissions. Other profiles remain permission-neutral until their module/action matrices are approved and implemented; lifecycle profiles cannot be manually assigned.
- Initial access-management authority is still supplied only by explicitly configured OIDC claim mapping. Missing configuration leaves `/api/v1/me` unauthorized and all administration routes unavailable.

- [ ] **Step 1: Write AAA service and MockMvc tests** for initial identity registration, role assignment/revocation, scope and reference validation, audit atomicity, no self-elevation, no lifecycle-role manual assignment, issuer isolation, effective admin permissions, no PII response, 401 without OIDC, 403 without read/write authority, and 409 stale version.
- [ ] **Step 2: Run tests and confirm RED** with missing service/routes or schema.
- [ ] **Step 3: Implement application service, i18n errors, role-aware authority resolution, response records, and explicit SecurityFilterChain matchers**; retain the final `/api/v1/admin/**` deny-all rule.
- [ ] **Step 4: Run focused tests and the full backend suite**, including MySQL contracts, and confirm GREEN.
- [ ] **Step 5: Commit** the backend access-management vertical slice.

### Task 4: Frontend access client and contracts

**Files:**
- Create: `frontend/src/features/access/roleAccessContracts.ts`
- Create: `frontend/src/features/access/roleAccessClient.ts`
- Create: `frontend/src/features/access/roleAccessClient.test.ts`
- Modify: `frontend/src/features/identity/identityContracts.ts`
- Modify: `frontend/src/features/identity/identityClient.ts` and tests.

**Interfaces:**
- `RoleAccessClient` exposes `listProfiles`, `listIdentities`, `listAssignments`, `assignRole`, and `revokeRole`; every method requires the in-memory access token and uses `credentials: 'omit'`, `cache: 'no-store'`, `AbortSignal`, strict response parsing, and current server-side authority.
- Role and scope types are closed unions matching the backend catalog; no frontend claim decoding or permission calculation is added.

- [ ] **Step 1: Write AAA client/parser tests** for valid payloads, unknown role/scope values, duplicate scopes, malformed identity, 401/403, network failure, cancellation, and no-store/no-credentials request options.
- [ ] **Step 2: Run the focused tests and confirm RED** because the client/contracts are absent.
- [ ] **Step 3: Implement the transport/client and extend `/api/v1/me` parsing** for the scoped role summary.
- [ ] **Step 4: Run focused tests and confirm GREEN**, then run the full frontend test suite.
- [ ] **Step 5: Commit** the client and contracts in `Universiry-frontend` `develop`.

### Task 5: Role and scope administration screen

**Files:**
- Create: `frontend/src/features/access/RoleAccessPage.tsx`
- Create: `frontend/src/features/access/RoleAccessPage.scss`
- Create: `frontend/src/features/access/RoleAccessPage.test.tsx`
- Modify: `frontend/src/App.tsx`, `frontend/src/App.test.tsx`, and `frontend/src/App.scss` only where needed for navigation.

**Interfaces:**
- `RoleAccessPage` receives `RoleAccessClient`, in-memory token, and read/write booleans derived exclusively from `/api/v1/me`.
- The console lists already-authenticated opaque subjects, role profiles, assignments and typed scopes; it requires source reference and validity dates, supports revocation with confirmation/version, and never requests names, emails, passwords, or token claims.
- `/#roles` and its navigation item are available only with `identity:roles:read`; direct URL access without that permission renders an access-denied state and sends no administration request.

- [ ] **Step 1: Write AAA component tests** for permission-hidden navigation, direct-route denial, empty/error/loading states, exact subject selection, valid scoped assignment, invalid/missing reference feedback, read-only behavior, and revoke conflict refresh without retrying the mutation.
- [ ] **Step 2: Run focused tests and confirm RED** because the route and component are absent.
- [ ] **Step 3: Implement the Spanish-first responsive SCSS console** using shared design tokens and accessible labels/status announcements.
- [ ] **Step 4: Run focused tests and confirm GREEN**, then run `npm test`, `npm run build`, and `npm run lint`.
- [ ] **Step 5: Commit** the full frontend increment and push `Universiry-frontend` `develop`.

### Task 6: Architecture sync and integrated verification

**Files:**
- Modify: `docs/architecture/c4.md`, `docs/architecture/data-model.md`, `docs/architecture/process-flows.md`, `docs/ROADMAP.md`, `AGENTS.md`, and the access proposal status.

- [ ] **Step 1: Update C4, data and process diagrams** to show OIDC issuer/subject, role assignments, typed scopes, audit, default-deny bootstrap and the staged permission boundary.
- [ ] **Step 2: Update the roadmap and agent guidance** to distinguish delivered identity/assignment infrastructure from institutional operation and from still-unbuilt admissions, student, teacher, grades, reservations and notification capabilities.
- [ ] **Step 3: Run link/diff checks and inspect both repositories/submodule status.**
- [ ] **Step 4: Run backend Maven verification with Java 25, MySQL 8.4 contracts, and frontend test/build/lint; inspect Compose health and the actual HTTP/UI response.**
- [ ] **Step 5: Update the backend submodule pointer to the tested frontend commit, commit the backend docs/pointer, push both `develop` branches, and inspect resulting CI runs.**

---

## Execution rulings

- Execute inline in the current `develop` checkout to honor the explicit request for continuous implementation, per-milestone integration to `develop`, and the live Compose preview. Preserve the unrelated untracked `backend/time,uptime,level,tags`, `backend/time,uptime,level,tags.0`, and `output/` artifacts.
- Keep OIDC and identity-role authority unconfigured in local Compose; use synthetic principals only inside tests.
- Do not claim full university role semantics: non-administrator profiles remain permission-neutral until each capability's actor–action–scope matrix is implemented and validated.
