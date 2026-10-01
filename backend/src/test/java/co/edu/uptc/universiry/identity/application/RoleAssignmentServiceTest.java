package co.edu.uptc.universiry.identity.application;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Mock
    private IdentityDirectory identities;

    @Mock
    private RoleAssignmentRepository assignments;

    private RoleAssignmentService service;

    @BeforeEach
    void set_up_service_with_a_fixed_institutional_clock() {
        service = new RoleAssignmentService(identities, assignments,
                Clock.fixed(NOW, ZoneId.of("America/Bogota")));
    }

    @Test
    void grants_a_scoped_profile_with_one_matching_audit_transition() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        AuthenticatedPrincipal target = principal("teacher-17");
        when(identities.find(actor)).thenReturn(Optional.of(registered(actor)));
        when(identities.find(target)).thenReturn(Optional.of(registered(target)));
        CreateRoleAssignmentCommand command = new CreateRoleAssignmentCommand(
                target,
                "TEACHER",
                Set.of(new AssignmentScope(ScopeKind.PROGRAM, UUID.randomUUID().toString())),
                TODAY,
                TODAY.plusMonths(6),
                "Acta institucional 42");

        // Act
        RoleAssignment created = service.assign(actor, command);

        // Assert
        ArgumentCaptor<RoleAssignment> assignment = ArgumentCaptor.forClass(RoleAssignment.class);
        ArgumentCaptor<AccessAuditEvent> audit = ArgumentCaptor.forClass(AccessAuditEvent.class);
        verify(assignments).create(assignment.capture(), audit.capture());
        assertEquals(target, created.target());
        assertEquals(RoleProfile.TEACHER, created.profile());
        assertEquals(AssignmentStatus.ACTIVE, created.status());
        assertEquals(1, created.version());
        assertEquals(actor, assignment.getValue().grantedBy());
        assertEquals(AccessAuditAction.GRANTED, audit.getValue().action());
        assertEquals(assignment.getValue().id(), audit.getValue().assignmentId());
        assertEquals(0, audit.getValue().previousVersion());
        assertEquals(1, audit.getValue().version());
    }

    @Test
    void rejects_self_elevation_before_writing_an_assignment_or_audit() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        CreateRoleAssignmentCommand command = new CreateRoleAssignmentCommand(
                actor, "ADMINISTRATOR", Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.assign(actor, command));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void rejects_manual_applicant_membership_before_writing() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        AuthenticatedPrincipal target = principal("applicant-17");
        CreateRoleAssignmentCommand command = new CreateRoleAssignmentCommand(
                target, "APPLICANT", Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.assign(actor, command));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void rejects_assignment_when_target_has_never_authenticated() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        AuthenticatedPrincipal target = principal("unknown-subject");
        when(identities.find(actor)).thenReturn(Optional.of(registered(actor)));
        when(identities.find(target)).thenReturn(Optional.empty());
        CreateRoleAssignmentCommand command = new CreateRoleAssignmentCommand(
                target, "TEACHER", Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, "Acta institucional 42");

        // Act / Assert
        assertThrows(IdentityNotRegisteredException.class, () -> service.assign(actor, command));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void rejects_an_overlapping_duplicate_profile_and_scope() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        AuthenticatedPrincipal target = principal("teacher-17");
        when(identities.find(actor)).thenReturn(Optional.of(registered(actor)));
        when(identities.find(target)).thenReturn(Optional.of(registered(target)));
        Set<AssignmentScope> scopes = Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null));
        RoleAssignment existing = new RoleAssignment(
                UUID.randomUUID(), target, RoleProfile.TEACHER, scopes,
                TODAY.minusDays(30), TODAY.plusDays(30), AssignmentStatus.ACTIVE, actor,
                new InstitutionalReference("Acta institucional 41"), NOW.minusSeconds(3600), 1);
        when(assignments.findAssignments(target)).thenReturn(java.util.List.of(existing));
        CreateRoleAssignmentCommand command = new CreateRoleAssignmentCommand(
                target, "TEACHER", scopes, TODAY, TODAY.plusMonths(6), "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.assign(actor, command));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void refuses_to_revoke_the_callers_own_assignment() {
        // Arrange
        AuthenticatedPrincipal actor = principal("teacher-17");
        UUID assignmentId = UUID.randomUUID();
        RoleAssignment ownAssignment = new RoleAssignment(
                assignmentId, actor, RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, AssignmentStatus.ACTIVE, principal("admin-1"),
                new InstitutionalReference("Acta institucional 42"), NOW, 1);
        when(assignments.findAssignment(assignmentId)).thenReturn(Optional.of(ownAssignment));
        when(identities.find(actor)).thenReturn(Optional.of(registered(actor)));

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.revoke(actor, assignmentId, 1, "Acta institucional 43"));
        verify(assignments, never()).revoke(any(), any(Long.class), any());
    }

    private static RegisteredIdentity registered(AuthenticatedPrincipal principal) {
        return new RegisteredIdentity(UUID.randomUUID(), principal, NOW);
    }

    private static AuthenticatedPrincipal principal(String subject) {
        return new AuthenticatedPrincipal("https://identity.example.edu", subject);
    }
}
