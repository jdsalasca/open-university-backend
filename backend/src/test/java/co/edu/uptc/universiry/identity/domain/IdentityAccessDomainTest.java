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

    private static final AuthenticatedPrincipal TARGET = new AuthenticatedPrincipal(
            "https://identity.example.edu", "opaque-subject-7f29");
    private static final AuthenticatedPrincipal GRANTOR = new AuthenticatedPrincipal(
            "https://identity.example.edu", "opaque-admin-118a");
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void identity_is_the_exact_issuer_and_subject_pair() {
        // Arrange
        AuthenticatedPrincipal samePair = new AuthenticatedPrincipal(
                "https://identity.example.edu", "opaque-subject-7f29");
        AuthenticatedPrincipal otherIssuer = new AuthenticatedPrincipal(
                "https://other.example.edu", "opaque-subject-7f29");
        AuthenticatedPrincipal otherSubject = new AuthenticatedPrincipal(
                "https://identity.example.edu", "opaque-subject-7F29");

        // Act + Assert
        assertEquals(TARGET, samePair);
        assertNotEquals(TARGET, otherIssuer);
        assertNotEquals(TARGET, otherSubject);
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
                UUID.randomUUID(), TARGET, RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.SITE, "site-1")),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), AssignmentStatus.ACTIVE,
                GRANTOR, new InstitutionalReference("acta-2026-18"), CREATED_AT, 1);
        ResourceDescriptor resource = new ResourceDescriptor(Map.of(ScopeKind.SITE, "site-1"));
        LocalDate lastValidDay = LocalDate.of(2026, 10, 1);
        LocalDate expiredDay = LocalDate.of(2026, 10, 2);
        RoleAssignment revoked = new RoleAssignment(
                active.id(), TARGET, RoleProfile.TEACHER, active.scopes(), active.validFrom(),
                active.validThrough(), AssignmentStatus.REVOKED, GRANTOR,
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
    void audit_event_records_a_single_monotonic_transition() {
        // Arrange
        UUID assignmentId = UUID.randomUUID();

        // Act + Assert
        AccessAuditEvent event = new AccessAuditEvent(
                UUID.randomUUID(), assignmentId, AccessAuditAction.GRANTED,
                GRANTOR, CREATED_AT, new InstitutionalReference("acta-2026-18"), 0, 1);
        assertEquals(assignmentId, event.assignmentId());
        assertThrows(IllegalArgumentException.class, () -> new AccessAuditEvent(
                UUID.randomUUID(), assignmentId, AccessAuditAction.REVOKED,
                GRANTOR, CREATED_AT, new InstitutionalReference("acta-2026-18"), 2, 2));
    }

    private static RoleAssignment assignment(
            RoleProfile profile,
            Set<AssignmentScope> scopes,
            AssignmentStatus status) {
        return new RoleAssignment(
                UUID.randomUUID(), TARGET, profile, scopes,
                LocalDate.of(2026, 10, 1), null, status,
                GRANTOR, new InstitutionalReference("acta-2026-18"), CREATED_AT, 1);
    }
}
