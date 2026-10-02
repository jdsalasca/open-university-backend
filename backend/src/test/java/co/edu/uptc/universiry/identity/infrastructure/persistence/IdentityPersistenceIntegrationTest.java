package co.edu.uptc.universiry.identity.infrastructure.persistence;

import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.identity.application.RoleAssignmentRepository;
import co.edu.uptc.universiry.identity.application.RoleAssignmentVersionConflictException;
import co.edu.uptc.universiry.identity.domain.AccessAuditAction;
import co.edu.uptc.universiry.identity.domain.AccessAuditEvent;
import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.AssignmentStatus;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.InstitutionalReference;
import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.identity.domain.RoleProfile;
import co.edu.uptc.universiry.identity.domain.ScopeKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class IdentityPersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final InstitutionalReference SOURCE = new InstitutionalReference("Acta sintética 18-2026");

    @Autowired
    private IdentityDirectory identityDirectory;

    @Autowired
    private RoleAssignmentRepository assignmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removes_only_synthetic_access_records_created_by_this_test_suite() {
        jdbcTemplate.update("DELETE FROM identity_access_audit_event");
        jdbcTemplate.update("DELETE FROM identity_role_assignment_scope");
        jdbcTemplate.update("DELETE FROM identity_role_assignment");
        jdbcTemplate.update("DELETE FROM institutional_identity");
        jdbcTemplate.update("DELETE FROM university_user");
    }

    @Test
    void migration_starts_without_seeded_identities_or_role_assignments() {
        // Arrange
        int identities = count("institutional_identity");
        int users = count("university_user");
        int assignments = count("identity_role_assignment");

        // Act + Assert
        assertEquals(0, identities);
        assertEquals(0, users);
        assertEquals(0, assignments);
    }

    @Test
    void registration_is_idempotent_for_exact_pair_and_isolates_issuer() {
        // Arrange
        String subject = "opaque-subject-" + UUID.randomUUID();
        AuthenticatedPrincipal first = principal("https://id-one.example.edu", subject);
        AuthenticatedPrincipal otherIssuer = principal("https://id-two.example.edu", subject);

        // Act
        RegisteredIdentity initial = identityDirectory.registerIfAbsent(first, NOW);
        RegisteredIdentity repeated = identityDirectory.registerIfAbsent(first, NOW.plusSeconds(60));
        RegisteredIdentity distinct = identityDirectory.registerIfAbsent(otherIssuer, NOW);

        // Assert
        assertEquals(initial.id(), repeated.id());
        assertEquals(initial.userId(), repeated.userId());
        assertEquals(NOW, repeated.firstSeenAt());
        assertNotEquals(initial.id(), distinct.id());
        assertNotEquals(initial.userId(), distinct.userId());
        assertEquals(2, count("institutional_identity"));
        assertEquals(2, count("university_user"));
        assertTrue(identityDirectory.userExists(initial.userId()));
        assertFalse(identityDirectory.userExists(UUID.randomUUID()));
    }

    @Test
    void concurrent_registration_of_the_same_pair_creates_one_user_and_one_binding() throws Exception {
        // Arrange
        AuthenticatedPrincipal principal = principal("https://id.example.edu", "concurrent-" + UUID.randomUUID());
        int contenders = 8;
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegisteredIdentity>> registrations = new ArrayList<>();
        try {
            for (int index = 0; index < contenders; index++) {
                registrations.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("concurrent registration did not start");
                    }
                    return identityDirectory.registerIfAbsent(principal, NOW);
                }));
            }

            // Act
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<RegisteredIdentity> results = new ArrayList<>();
            for (Future<RegisteredIdentity> registration : registrations) {
                results.add(registration.get(10, TimeUnit.SECONDS));
            }

            // Assert
            assertEquals(1, results.stream().map(RegisteredIdentity::id).distinct().count());
            assertEquals(1, results.stream().map(RegisteredIdentity::userId).distinct().count());
            assertEquals(1, count("institutional_identity"));
            assertEquals(1, count("university_user"));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void subject_prefix_search_returns_only_registered_opaque_identities() {
        // Arrange
        String prefix = "subject-prefix-";
        identityDirectory.registerIfAbsent(principal("https://id.example.edu", prefix + "alpha"), NOW);
        identityDirectory.registerIfAbsent(principal("https://id.example.edu", prefix + "beta"), NOW);
        identityDirectory.registerIfAbsent(principal("https://id.example.edu", "unrelated-subject"), NOW);

        // Act
        List<RegisteredIdentity> results = identityDirectory.findBySubjectPrefix(prefix, 10);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(identity -> identity.principal().subject().startsWith(prefix)));
        assertThrows(IllegalArgumentException.class, () -> identityDirectory.findBySubjectPrefix("", 10));
        assertThrows(IllegalArgumentException.class, () -> identityDirectory.findBySubjectPrefix(prefix, 0));
    }

    @Test
    void assignment_and_typed_academic_scopes_persist_with_one_audit_event() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "teacher-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "administrator-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(target, NOW);
        identityDirectory.registerIfAbsent(grantor, NOW);
        UUID siteId = createSite();
        UUID facultyId = createFaculty();
        UUID programId = createProgram();
        RoleAssignment assignment = assignment(target, grantor, Set.of(
                new AssignmentScope(ScopeKind.SITE, siteId.toString()),
                new AssignmentScope(ScopeKind.FACULTY, facultyId.toString()),
                new AssignmentScope(ScopeKind.PROGRAM, programId.toString())));
        AccessAuditEvent grantEvent = event(assignment.id(), AccessAuditAction.GRANTED, grantor, 0, 1);

        // Act
        assignmentRepository.create(assignment, grantEvent);
        UUID targetUserId = userId(target);
        List<RoleAssignment> allAssignments = assignmentRepository.findAssignments(targetUserId);
        List<RoleAssignment> activeAssignments = assignmentRepository.findActiveAssignments(targetUserId, TODAY);

        // Assert
        assertEquals(1, allAssignments.size());
        assertEquals(Set.of(
                new AssignmentScope(ScopeKind.SITE, siteId.toString()),
                new AssignmentScope(ScopeKind.FACULTY, facultyId.toString()),
                new AssignmentScope(ScopeKind.PROGRAM, programId.toString())), allAssignments.getFirst().scopes());
        assertEquals(1, activeAssignments.size());
        assertEquals(1, count("identity_access_audit_event"));
        String expectedActorIdentityId = identityDirectory.find(grantor).orElseThrow().id().toString();
        assertEquals(expectedActorIdentityId, jdbcTemplate.queryForObject(
                "SELECT actor_identity_id FROM identity_access_audit_event WHERE assignment_id = ?",
                String.class, assignment.id().toString()));
        assertEquals(userId(grantor).toString(), jdbcTemplate.queryForObject(
                "SELECT actor_user_id FROM identity_access_audit_event WHERE assignment_id = ?",
                String.class, assignment.id().toString()));
    }

    @Test
    void audit_event_rejects_a_federated_binding_from_another_canonical_user_atomically() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "teacher-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "manager-" + UUID.randomUUID());
        AuthenticatedPrincipal unrelatedActor = principal("https://id.example.edu", "unrelated-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(target, NOW);
        RegisteredIdentity registeredGrantor = identityDirectory.registerIfAbsent(grantor, NOW);
        RegisteredIdentity unrelatedBinding = identityDirectory.registerIfAbsent(unrelatedActor, NOW);
        RoleAssignment assignment = assignment(target, grantor,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)));
        AccessAuditEvent mismatchedEvent = new AccessAuditEvent(
                UUID.randomUUID(), assignment.id(), AccessAuditAction.GRANTED,
                unrelatedBinding.id(), registeredGrantor.userId(), NOW, SOURCE, 0, 1);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> assignmentRepository.create(assignment, mismatchedEvent));
        assertEquals(0, count("identity_role_assignment"));
        assertEquals(0, count("identity_access_audit_event"));
    }

    @Test
    void assignments_are_shared_by_oidc_aliases_and_isolated_by_canonical_user_id() {
        // Arrange
        AuthenticatedPrincipal originalBinding = principal(
                "https://id.example.edu", "teacher-original-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal(
                "https://id.example.edu", "administrator-" + UUID.randomUUID());
        RegisteredIdentity original = identityDirectory.registerIfAbsent(originalBinding, NOW);
        RegisteredIdentity registeredGrantor = identityDirectory.registerIfAbsent(grantor, NOW);
        RoleAssignment assignment = new RoleAssignment(
                UUID.randomUUID(), original.userId(), RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)), TODAY, null,
                AssignmentStatus.ACTIVE, registeredGrantor.userId(), SOURCE, NOW, 1);
        assignmentRepository.create(assignment, event(
                assignment.id(), AccessAuditAction.GRANTED, grantor, 0, 1));

        AuthenticatedPrincipal aliasBinding = principal(
                "https://alternate-id.example.edu", "teacher-alias-" + UUID.randomUUID());
        insertAliasBinding(UUID.randomUUID(), original.userId(), aliasBinding);
        RegisteredIdentity otherUser = identityDirectory.registerIfAbsent(
                principal("https://id.example.edu", "unrelated-teacher-" + UUID.randomUUID()), NOW);

        // Act
        RegisteredIdentity alias = identityDirectory.find(aliasBinding).orElseThrow();
        List<RoleAssignment> sharedAssignments = assignmentRepository.findAssignments(alias.userId());

        // Assert
        assertEquals(original.userId(), alias.userId());
        assertEquals(1, sharedAssignments.size());
        assertEquals(assignment.id(), sharedAssignments.getFirst().id());
        assertTrue(assignmentRepository.findAssignments(otherUser.userId()).isEmpty());
    }

    @Test
    void missing_scope_foreign_key_rolls_back_assignment_and_audit() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "teacher-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "administrator-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(target, NOW);
        identityDirectory.registerIfAbsent(grantor, NOW);
        RoleAssignment assignment = assignment(target, grantor, Set.of(
                new AssignmentScope(ScopeKind.SITE, UUID.randomUUID().toString())));

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> assignmentRepository.create(assignment, event(assignment.id(), AccessAuditAction.GRANTED, grantor, 0, 1)));
        assertEquals(0, count("identity_role_assignment"));
        assertEquals(0, count("identity_access_audit_event"));
    }

    @Test
    void duplicate_audit_event_rolls_back_the_preceding_assignment_and_scope_writes() {
        // Arrange
        AuthenticatedPrincipal firstTarget = principal("https://id.example.edu", "teacher-" + UUID.randomUUID());
        AuthenticatedPrincipal secondTarget = principal("https://id.example.edu", "teacher-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "administrator-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(firstTarget, NOW);
        identityDirectory.registerIfAbsent(secondTarget, NOW);
        identityDirectory.registerIfAbsent(grantor, NOW);
        RoleAssignment firstAssignment = assignment(firstTarget, grantor,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)));
        RoleAssignment secondAssignment = assignment(secondTarget, grantor,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)));
        UUID duplicateAuditId = UUID.randomUUID();
        assignmentRepository.create(firstAssignment, event(
                duplicateAuditId, firstAssignment.id(), AccessAuditAction.GRANTED, grantor, 0, 1));

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class, () -> assignmentRepository.create(
                secondAssignment, event(duplicateAuditId, secondAssignment.id(),
                        AccessAuditAction.GRANTED, grantor, 0, 1)));
        assertEquals(1, count("identity_role_assignment"));
        assertEquals(1, count("identity_role_assignment_scope"));
        assertEquals(1, count("identity_access_audit_event"));
        assertTrue(assignmentRepository.findAssignments(userId(secondTarget)).isEmpty());
    }

    @Test
    void database_rejects_unknown_role_profiles() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "target-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "grantor-" + UUID.randomUUID());
        RegisteredIdentity targetRecord = identityDirectory.registerIfAbsent(target, NOW);
        RegisteredIdentity grantorRecord = identityDirectory.registerIfAbsent(grantor, NOW);

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> insertAssignmentWithoutDomainValidation("SUPERUSER", targetRecord, grantorRecord));
    }

    @Test
    void database_rejects_lifecycle_profiles_until_their_verified_source_exists() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "target-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "grantor-" + UUID.randomUUID());
        RegisteredIdentity targetRecord = identityDirectory.registerIfAbsent(target, NOW);
        RegisteredIdentity grantorRecord = identityDirectory.registerIfAbsent(grantor, NOW);

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> insertAssignmentWithoutDomainValidation("APPLICANT", targetRecord, grantorRecord));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertAssignmentWithoutDomainValidation("ADMITTED", targetRecord, grantorRecord));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertAssignmentWithoutDomainValidation("STUDENT", targetRecord, grantorRecord));
    }

    @Test
    void stale_revocation_keeps_current_assignment_and_audit_history_intact() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "administrator-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "access-manager-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(target, NOW);
        identityDirectory.registerIfAbsent(grantor, NOW);
        RoleAssignment assignment = assignment(target, grantor,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)), RoleProfile.ADMINISTRATOR);
        assignmentRepository.create(assignment, event(assignment.id(), AccessAuditAction.GRANTED, grantor, 0, 1));
        AccessAuditEvent staleEvent = event(assignment.id(), AccessAuditAction.REVOKED, grantor, 1, 2);

        // Act + Assert
        assertThrows(RoleAssignmentVersionConflictException.class,
                () -> assignmentRepository.revoke(assignment.id(), 0, staleEvent));
        assertEquals(AssignmentStatus.ACTIVE, assignmentRepository.findAssignments(userId(target)).getFirst().status());
        assertEquals(1, count("identity_access_audit_event"));
    }

    @Test
    void revocation_appends_a_second_event_and_changes_the_version_once() {
        // Arrange
        AuthenticatedPrincipal target = principal("https://id.example.edu", "administrator-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("https://id.example.edu", "access-manager-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(target, NOW);
        identityDirectory.registerIfAbsent(grantor, NOW);
        RoleAssignment assignment = assignment(target, grantor,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)), RoleProfile.ADMINISTRATOR);
        assignmentRepository.create(assignment, event(assignment.id(), AccessAuditAction.GRANTED, grantor, 0, 1));

        // Act
        RoleAssignment revoked = assignmentRepository.revoke(
                assignment.id(), 1, event(assignment.id(), AccessAuditAction.REVOKED, grantor, 1, 2));

        // Assert
        assertEquals(AssignmentStatus.REVOKED, revoked.status());
        assertEquals(2, revoked.version());
        assertEquals(2, count("identity_access_audit_event"));
        assertEquals(0, assignmentRepository.findActiveAssignments(userId(target), TODAY).size());
        String expectedActorIdentityId = identityDirectory.find(grantor).orElseThrow().id().toString();
        assertEquals(expectedActorIdentityId, jdbcTemplate.queryForObject("""
                SELECT actor_identity_id FROM identity_access_audit_event
                WHERE assignment_id = ? AND action_key = 'REVOKED'
                """, String.class, assignment.id().toString()));
    }

    private RoleAssignment assignment(
            AuthenticatedPrincipal target,
            AuthenticatedPrincipal grantor,
            Set<AssignmentScope> scopes) {
        return assignment(target, grantor, scopes, RoleProfile.TEACHER);
    }

    private RoleAssignment assignment(
            AuthenticatedPrincipal target,
            AuthenticatedPrincipal grantor,
            Set<AssignmentScope> scopes,
            RoleProfile profile) {
        return new RoleAssignment(UUID.randomUUID(), userId(target), profile, scopes,
                TODAY, null, AssignmentStatus.ACTIVE, userId(grantor), SOURCE, NOW, 1);
    }

    private AccessAuditEvent event(
            UUID assignmentId,
            AccessAuditAction action,
            AuthenticatedPrincipal actor,
            long previousVersion,
            long version) {
        RegisteredIdentity registeredActor = identityDirectory.find(actor).orElseThrow();
        return new AccessAuditEvent(UUID.randomUUID(), assignmentId, action,
                registeredActor.id(), registeredActor.userId(), NOW, SOURCE, previousVersion, version);
    }

    private AccessAuditEvent event(
            UUID eventId,
            UUID assignmentId,
            AccessAuditAction action,
            AuthenticatedPrincipal actor,
            long previousVersion,
            long version) {
        RegisteredIdentity registeredActor = identityDirectory.find(actor).orElseThrow();
        return new AccessAuditEvent(eventId, assignmentId, action,
                registeredActor.id(), registeredActor.userId(), NOW, SOURCE, previousVersion, version);
    }

    private UUID createSite() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_site (
                    site_id, site_code, site_type, display_name, display_order, status,
                    valid_from, valid_through, created_at
                ) VALUES (?, ?, 'CAMPUS', 'Sede sintética', 0, 'ACTIVE', ?, NULL, ?)
                """, id.toString(), "SITE-" + id, TODAY, NOW);
        return id;
    }

    private UUID createFaculty() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_organization_unit (
                    organization_unit_id, unit_code, unit_type, display_name, display_order,
                    status, valid_from, valid_through, created_at
                ) VALUES (?, ?, 'FACULTY', 'Facultad sintética', 0, 'ACTIVE', ?, NULL, ?)
                """, id.toString(), "FAC-" + id, TODAY, NOW);
        return id;
    }

    private UUID createProgram() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_program (
                    program_id, program_code, academic_level, study_modality, campus_code, created_at
                ) VALUES (?, ?, 'PREGRADO', 'PRESENCIAL', 'SYNTHETIC', ?)
                """, id.toString(), "PROGRAM-" + id, NOW);
        return id;
    }

    private void insertAssignmentWithoutDomainValidation(
            String profileKey,
            RegisteredIdentity target,
            RegisteredIdentity grantor) {
        jdbcTemplate.update("""
                INSERT INTO identity_role_assignment (
                    assignment_id, target_user_id, profile_key, status, valid_from, valid_through,
                    source_reference, granted_by_user_id, created_at, version
                ) VALUES (?, ?, ?, 'ACTIVE', ?, NULL, ?, ?, ?, 1)
                """, UUID.randomUUID().toString(), target.userId().toString(), profileKey,
                TODAY, SOURCE.value(), grantor.userId().toString(), NOW);
    }

    private void insertAliasBinding(UUID identityId, UUID userId, AuthenticatedPrincipal principal) {
        byte[] issuerDigest;
        try {
            issuerDigest = MessageDigest.getInstance("SHA-256")
                    .digest(principal.issuer().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        jdbcTemplate.update("""
                INSERT INTO institutional_identity (
                    identity_id, user_id, issuer, subject, issuer_sha256, first_seen_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, identityId.toString(), userId.toString(), principal.issuer(),
                principal.subject().getBytes(StandardCharsets.US_ASCII), issuerDigest, NOW);
    }

    private UUID userId(AuthenticatedPrincipal principal) {
        return identityDirectory.find(principal).orElseThrow().userId();
    }

    private static AuthenticatedPrincipal principal(String issuer, String subject) {
        return new AuthenticatedPrincipal(issuer, subject);
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
