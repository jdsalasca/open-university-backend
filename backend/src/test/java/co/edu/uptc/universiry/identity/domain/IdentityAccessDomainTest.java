package co.edu.uptc.universiry.identity.domain;

import co.edu.uptc.universiry.security.ApplicationPermission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityAccessDomainTest {

    private static final UUID TARGET_USER_ID = UUID.fromString("0b394286-422f-4ba9-96e9-51896e224334");
    private static final UUID GRANTOR_USER_ID = UUID.fromString("a2f05308-311a-4a08-999f-f045024cdd41");
    private static final UUID GRANTOR_IDENTITY_ID = UUID.fromString("51f05308-311a-4a08-999f-f045024cdd41");
    private static final AuthenticatedPrincipal TARGET_PRINCIPAL = new AuthenticatedPrincipal(
            "https://identity.example.edu", "opaque-subject-7f29");
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void identity_is_the_exact_issuer_and_subject_pair() {
        // Arrange
        AuthenticatedPrincipal samePair = new AuthenticatedPrincipal(
                "https://identity.example.edu", "opaque-subject-7f29");
        AuthenticatedPrincipal otherIssuer = new AuthenticatedPrincipal(
                "https://other.example.edu", "opaque-subject-7f29");
        AuthenticatedPrincipal differentIssuerCase = new AuthenticatedPrincipal(
                "https://Identity.example.edu", "opaque-subject-7f29");
        AuthenticatedPrincipal otherSubject = new AuthenticatedPrincipal(
                "https://identity.example.edu", "opaque-subject-7F29");

        // Act + Assert
        assertEquals(TARGET_PRINCIPAL, samePair);
        assertNotEquals(TARGET_PRINCIPAL, otherIssuer);
        assertNotEquals(TARGET_PRINCIPAL, differentIssuerCase);
        assertNotEquals(TARGET_PRINCIPAL, otherSubject);
    }

    @Test
    void rejects_blank_oversized_or_controlled_identity_values() {
        // Arrange
        String oversized = "x".repeat(2049);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedPrincipal(" ", "sub"));
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedPrincipal("issuer", "\tsub"));
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedPrincipal("issuer\nvalue", "sub"));
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedPrincipal("issuer", oversized));
    }

    @Test
    void subject_obeys_the_case_sensitive_ascii_oidc_limit() {
        // Arrange
        String oversizedSubject = "s".repeat(256);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedPrincipal("https://identity.example.edu", oversizedSubject));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedPrincipal("https://identity.example.edu", "sübject"));
        assertNotEquals(
                new AuthenticatedPrincipal("https://identity.example.edu", "CaseSensitive"),
                new AuthenticatedPrincipal("https://identity.example.edu", "casesensitive"));
    }

    @Test
    void issuer_is_an_exact_https_url_without_query_or_fragment() {
        // Arrange
        String subject = "opaque-subject-7f29";

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedPrincipal("not-an-issuer", subject));
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedPrincipal("http://id.example.edu", subject));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedPrincipal("https://id.example.edu?tenant=one", subject));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedPrincipal("https://id.example.edu/#issuer", subject));
        assertEquals("https://Identity.example.edu/path", new AuthenticatedPrincipal(
                "https://Identity.example.edu/path", subject).issuer());
    }

    @Test
    void role_catalog_is_closed_and_has_eight_stable_profiles() {
        // Arrange
        Set<String> expectedKeys = Set.of(
                "APPLICANT", "ADMITTED", "STUDENT", "TEACHER",
                "ADMINISTRATIVE", "ADMISSIONS", "DIRECTIVE", "ADMINISTRATOR");

        // Act
        List<RoleProfile> catalog = RoleProfile.catalog();

        // Assert
        assertEquals(8, catalog.size());
        assertEquals(expectedKeys, catalog.stream().map(RoleProfile::key).collect(java.util.stream.Collectors.toSet()));
        assertThrows(IllegalArgumentException.class, () -> RoleProfile.fromKey("SUPERUSER"));
    }

    @Test
    void lifecycle_profiles_are_not_manually_assignable_and_administrator_is_least_privileged() {
        // Arrange
        Set<ApplicationPermission> administratorPermissions = Set.of(
                ApplicationPermission.IDENTITY_ROLES_READ,
                ApplicationPermission.IDENTITY_ROLES_WRITE);

        // Act + Assert
        assertFalse(RoleProfile.APPLICANT.manuallyAssignable());
        assertFalse(RoleProfile.ADMITTED.manuallyAssignable());
        assertFalse(RoleProfile.STUDENT.manuallyAssignable());
        assertEquals(administratorPermissions, RoleProfile.ADMINISTRATOR.permissions());
        assertTrue(RoleProfile.catalog().stream()
                .filter(profile -> profile != RoleProfile.ADMINISTRATOR)
                .allMatch(profile -> profile.permissions().isEmpty()));
    }

    @Test
    void administrator_assignment_requires_exactly_university_scope() {
        // Arrange
        AssignmentScope faculty = new AssignmentScope(ScopeKind.FACULTY, "unit-fac-12");
        AssignmentScope university = new AssignmentScope(ScopeKind.UNIVERSITY, null);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> assignment(RoleProfile.ADMINISTRATOR, Set.of(faculty), AssignmentStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> assignment(RoleProfile.ADMINISTRATOR, Set.of(university, faculty), AssignmentStatus.ACTIVE));
        assertEquals(Set.of(ScopeKind.UNIVERSITY), RoleProfile.ADMINISTRATOR.allowedScopeKinds());
    }

    @Test
    void one_assignment_cannot_repeat_a_scope_kind_with_conflicting_references() {
        // Arrange
        Set<AssignmentScope> conflictingProgramScopes = Set.of(
                new AssignmentScope(ScopeKind.PROGRAM, "program-23"),
                new AssignmentScope(ScopeKind.PROGRAM, "program-24"));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> assignment(RoleProfile.TEACHER, conflictingProgramScopes, AssignmentStatus.ACTIVE));
    }

    @Test
    void scope_reference_rules_are_typed_and_bounded() {
        // Arrange
        String oversized = "r".repeat(257);

        // Act + Assert
        assertEquals(new AssignmentScope(ScopeKind.UNIVERSITY, null),
                new AssignmentScope(ScopeKind.UNIVERSITY, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AssignmentScope(ScopeKind.UNIVERSITY, "university-1"));
        assertThrows(IllegalArgumentException.class,
                () -> new AssignmentScope(ScopeKind.PROGRAM, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AssignmentScope(ScopeKind.FACULTY, "  "));
        assertThrows(IllegalArgumentException.class,
                () -> new AssignmentScope(ScopeKind.JOB_APPOINTMENT, oversized));
    }

    @Test
    void institutional_reference_is_validated_once_as_a_domain_value() {
        // Arrange
        String oversized = "a".repeat(513);

        // Act + Assert
        assertEquals("Acta 18-2026", new InstitutionalReference("Acta 18-2026").value());
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalReference(" "));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalReference(oversized));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalReference("acta\n18"));
    }

    @Test
    void all_scopes_in_one_assignment_must_match_the_resource() {
        // Arrange
        RoleAssignment assignment = assignment(RoleProfile.TEACHER, Set.of(
                new AssignmentScope(ScopeKind.FACULTY, "faculty-5"),
                new AssignmentScope(ScopeKind.PROGRAM, "program-23")), AssignmentStatus.ACTIVE);
        ResourceDescriptor matchingResource = new ResourceDescriptor(Map.of(
                ScopeKind.FACULTY, "faculty-5", ScopeKind.PROGRAM, "program-23"));
        ResourceDescriptor differentProgram = new ResourceDescriptor(Map.of(
                ScopeKind.FACULTY, "faculty-5", ScopeKind.PROGRAM, "program-24"));

        // Act + Assert
        assertTrue(assignment.matches(matchingResource, LocalDate.of(2026, 10, 1)));
        assertFalse(assignment.matches(differentProgram, LocalDate.of(2026, 10, 1)));
    }

    @Test
    void assignment_validity_is_inclusive_and_revocation_stops_matching() {
        // Arrange
        RoleAssignment active = new RoleAssignment(
                UUID.randomUUID(), TARGET_USER_ID, RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.SITE, "site-1")),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), AssignmentStatus.ACTIVE,
                GRANTOR_USER_ID, new InstitutionalReference("acta-2026-18"), CREATED_AT, 1);
        ResourceDescriptor resource = new ResourceDescriptor(Map.of(ScopeKind.SITE, "site-1"));
        LocalDate lastValidDay = LocalDate.of(2026, 10, 1);
        LocalDate expiredDay = LocalDate.of(2026, 10, 2);
        RoleAssignment revoked = new RoleAssignment(
                active.id(), TARGET_USER_ID, RoleProfile.TEACHER, active.scopes(), active.validFrom(),
                active.validThrough(), AssignmentStatus.REVOKED, GRANTOR_USER_ID,
                active.sourceReference(), CREATED_AT, 2);

        // Act + Assert
        assertTrue(active.matches(resource, lastValidDay));
        assertFalse(active.matches(resource, expiredDay));
        assertFalse(revoked.matches(resource, lastValidDay));
    }

    @Test
    void separate_valid_assignments_combine_with_or_semantics() {
        // Arrange
        RoleAssignment first = assignment(RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.SITE, "site-1")), AssignmentStatus.ACTIVE);
        RoleAssignment second = assignment(RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.SITE, "site-2")), AssignmentStatus.ACTIVE);
        ResourceDescriptor resource = new ResourceDescriptor(Map.of(ScopeKind.SITE, "site-2"));

        // Act
        boolean anyAssignmentMatches = List.of(first, second).stream()
                .anyMatch(item -> item.matches(resource, LocalDate.of(2026, 10, 1)));

        // Assert
        assertTrue(anyAssignmentMatches);
    }

    @Test
    void role_assignment_rejects_matching_canonical_target_and_grantor_ids() {
        // Arrange
        UUID sameUserId = UUID.randomUUID();

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new RoleAssignment(
                UUID.randomUUID(), sameUserId, RoleProfile.ADMINISTRATOR,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                LocalDate.of(2026, 10, 1), null, AssignmentStatus.ACTIVE, sameUserId,
                new InstitutionalReference("acta-2026-18"), CREATED_AT, 1));
    }

    @Test
    void audit_event_records_a_single_monotonic_transition() {
        // Arrange
        UUID assignmentId = UUID.randomUUID();

        // Act + Assert
        AccessAuditEvent event = new AccessAuditEvent(
                UUID.randomUUID(), assignmentId, AccessAuditAction.GRANTED,
                GRANTOR_IDENTITY_ID, GRANTOR_USER_ID,
                CREATED_AT, new InstitutionalReference("acta-2026-18"), 0, 1);
        assertEquals(assignmentId, event.assignmentId());
        assertEquals(GRANTOR_IDENTITY_ID, event.actorIdentityId());
        assertThrows(IllegalArgumentException.class, () -> new AccessAuditEvent(
                UUID.randomUUID(), assignmentId, AccessAuditAction.REVOKED,
                GRANTOR_IDENTITY_ID, GRANTOR_USER_ID,
                CREATED_AT, new InstitutionalReference("acta-2026-18"), 2, 2));
        assertThrows(IllegalArgumentException.class, () -> new AccessAuditEvent(
                UUID.randomUUID(), assignmentId, AccessAuditAction.REVOKED,
                null, GRANTOR_USER_ID, CREATED_AT, new InstitutionalReference("acta-2026-18"), 0, 1));
    }

    private static RoleAssignment assignment(
            RoleProfile profile,
            Set<AssignmentScope> scopes,
            AssignmentStatus status) {
        return new RoleAssignment(
                UUID.randomUUID(), TARGET_USER_ID, profile, scopes,
                LocalDate.of(2026, 10, 1), null, status,
                GRANTOR_USER_ID, new InstitutionalReference("acta-2026-18"), CREATED_AT, 1);
    }
}
