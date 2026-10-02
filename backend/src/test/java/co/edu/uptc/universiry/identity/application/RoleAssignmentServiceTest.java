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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void grants_a_scoped_profile_and_audit_transition_to_a_canonical_user() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        UUID actorUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        RegisteredIdentity registeredActor = registered(actor, actorUserId);
        when(identities.find(actor)).thenReturn(Optional.of(registeredActor));
        when(identities.userExists(targetUserId)).thenReturn(true);
        when(assignments.findAssignments(targetUserId)).thenReturn(List.of());
        CreateRoleAssignmentCommand command = command(targetUserId, "TEACHER");

        // Act
        RoleAssignment created = service.assign(actor, command);

        // Assert
        ArgumentCaptor<RoleAssignment> assignment = ArgumentCaptor.forClass(RoleAssignment.class);
        ArgumentCaptor<AccessAuditEvent> audit = ArgumentCaptor.forClass(AccessAuditEvent.class);
        verify(assignments).create(assignment.capture(), audit.capture());
        assertEquals(targetUserId, created.targetUserId());
        assertEquals(RoleProfile.TEACHER, created.profile());
        assertEquals(AssignmentStatus.ACTIVE, created.status());
        assertEquals(1, created.version());
        assertEquals(actorUserId, assignment.getValue().grantedByUserId());
        assertEquals(actorUserId, audit.getValue().actorUserId());
        assertEquals(registeredActor.id(), audit.getValue().actorIdentityId());
        assertEquals(AccessAuditAction.GRANTED, audit.getValue().action());
        assertEquals(assignment.getValue().id(), audit.getValue().assignmentId());
        assertEquals(0, audit.getValue().previousVersion());
        assertEquals(1, audit.getValue().version());
    }

    @Test
    void rejects_self_elevation_when_actor_uses_an_alternate_oidc_binding() {
        // Arrange
        AuthenticatedPrincipal actorAlias = principal("role-manager-alias");
        UUID canonicalUserId = UUID.randomUUID();
        when(identities.find(actorAlias)).thenReturn(Optional.of(registered(actorAlias, canonicalUserId)));

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.assign(actorAlias, command(canonicalUserId, "ADMINISTRATOR")));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void rejects_manual_applicant_membership_before_writing() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.assign(actor, command(UUID.randomUUID(), "APPLICANT")));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void rejects_assignment_when_target_user_does_not_exist() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        when(identities.find(actor)).thenReturn(Optional.of(registered(actor, UUID.randomUUID())));
        UUID unknownUserId = UUID.randomUUID();
        when(identities.userExists(unknownUserId)).thenReturn(false);

        // Act / Assert
        assertThrows(IdentityNotRegisteredException.class,
                () -> service.assign(actor, command(unknownUserId, "TEACHER")));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void rejects_an_overlapping_duplicate_profile_and_scope() {
        // Arrange
        AuthenticatedPrincipal actor = principal("role-manager");
        UUID actorUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(identities.find(actor)).thenReturn(Optional.of(registered(actor, actorUserId)));
        when(identities.userExists(targetUserId)).thenReturn(true);
        Set<AssignmentScope> scopes = Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null));
        RoleAssignment existing = new RoleAssignment(
                UUID.randomUUID(), targetUserId, RoleProfile.TEACHER, scopes,
                TODAY.minusDays(30), TODAY.plusDays(30), AssignmentStatus.ACTIVE, UUID.randomUUID(),
                new InstitutionalReference("Acta institucional 41"), NOW.minusSeconds(3600), 1);
        when(assignments.findAssignments(targetUserId)).thenReturn(List.of(existing));
        CreateRoleAssignmentCommand command = new CreateRoleAssignmentCommand(
                targetUserId, "TEACHER", scopes, TODAY, TODAY.plusMonths(6), "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.assign(actor, command));
        verify(assignments, never()).create(any(), any());
    }

    @Test
    void refuses_to_revoke_own_assignment_through_an_alternate_oidc_binding() {
        // Arrange
        AuthenticatedPrincipal actorAlias = principal("teacher-alias");
        UUID canonicalUserId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        RoleAssignment ownAssignment = new RoleAssignment(
                assignmentId, canonicalUserId, RoleProfile.TEACHER,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, AssignmentStatus.ACTIVE, UUID.randomUUID(),
                new InstitutionalReference("Acta institucional 42"), NOW, 1);
        when(assignments.findAssignment(assignmentId)).thenReturn(Optional.of(ownAssignment));
        when(identities.find(actorAlias)).thenReturn(Optional.of(registered(actorAlias, canonicalUserId)));

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.revoke(actorAlias, assignmentId, 1, "Acta institucional 43"));
        verify(assignments, never()).revoke(any(), any(Long.class), any());
    }

    @Test
    void passes_unknown_user_queries_to_the_repository_as_an_empty_result() {
        // Arrange
        UUID unknownUserId = UUID.randomUUID();
        when(assignments.findAssignments(unknownUserId)).thenReturn(List.of());

        // Act
        List<RoleAssignment> result = service.assignmentsFor(unknownUserId);

        // Assert
        verify(assignments).findAssignments(unknownUserId);
        assertTrue(result.isEmpty());
    }

    private static CreateRoleAssignmentCommand command(UUID targetUserId, String profileKey) {
        Set<AssignmentScope> scopes = profileKey.equals("ADMINISTRATOR")
                ? Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null))
                : Set.of(new AssignmentScope(ScopeKind.PROGRAM, UUID.randomUUID().toString()));
        return new CreateRoleAssignmentCommand(
                targetUserId, profileKey, scopes, TODAY, TODAY.plusMonths(6), "Acta institucional 42");
    }

    private static RegisteredIdentity registered(AuthenticatedPrincipal principal, UUID userId) {
        return new RegisteredIdentity(UUID.randomUUID(), userId, principal, NOW);
    }

    private static AuthenticatedPrincipal principal(String subject) {
        return new AuthenticatedPrincipal("https://identity.example.edu", subject);
    }
}
