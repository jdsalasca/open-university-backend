package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AccessAuditAction;
import co.edu.uptc.universiry.identity.domain.AccessAuditEvent;
import co.edu.uptc.universiry.identity.domain.AssignmentStatus;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.InstitutionalReference;
import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.identity.domain.RoleProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RoleAssignmentService {

    private final IdentityDirectory identities;
    private final RoleAssignmentRepository assignments;
    private final Clock clock;

    public RoleAssignmentService(IdentityDirectory identities, RoleAssignmentRepository assignments, Clock clock) {
        this.identities = identities;
        this.assignments = assignments;
        this.clock = clock;
    }

    public List<RoleProfile> roleProfiles() {
        return RoleProfile.catalog();
    }

    public List<RegisteredIdentity> searchIdentities(String subjectPrefix, int limit) {
        return identities.findBySubjectPrefix(subjectPrefix, limit);
    }

    public List<RoleAssignment> assignmentsFor(AuthenticatedPrincipal target) {
        return assignments.findAssignments(target);
    }

    @Transactional
    public RoleAssignment assign(AuthenticatedPrincipal actor, CreateRoleAssignmentCommand command) {
        if (actor == null || command == null || command.target() == null) {
            throw new IllegalArgumentException("actor and role assignment command are required");
        }
        if (actor.equals(command.target())) {
            throw new IllegalArgumentException("a principal cannot assign a role to itself");
        }
        RoleProfile profile = RoleProfile.fromKey(command.profileKey());
        if (!profile.manuallyAssignable()) {
            throw new IllegalArgumentException("lifecycle profiles must come from their verified source");
        }
        requireRegistered(actor);
        if (identities.find(command.target()).isEmpty()) {
            throw new IdentityNotRegisteredException();
        }
        InstitutionalReference reference = new InstitutionalReference(command.sourceReference());
        var now = clock.instant();
        RoleAssignment assignment = new RoleAssignment(
                UUID.randomUUID(), command.target(), profile, command.scopes(), command.validFrom(),
                command.validThrough(), AssignmentStatus.ACTIVE, actor, reference, now, 1);
        boolean overlappingDuplicate = assignments.findAssignments(command.target()).stream()
                .filter(existing -> existing.status() == AssignmentStatus.ACTIVE)
                .filter(existing -> existing.profile() == profile && existing.scopes().equals(assignment.scopes()))
                .anyMatch(existing -> validityOverlaps(
                        assignment.validFrom(), assignment.validThrough(), existing.validFrom(), existing.validThrough()));
        if (overlappingDuplicate) {
            throw new IllegalArgumentException("an overlapping assignment with the same profile and scope already exists");
        }
        AccessAuditEvent auditEvent = new AccessAuditEvent(
                UUID.randomUUID(), assignment.id(), AccessAuditAction.GRANTED, actor,
                now, reference, 0, 1);
        assignments.create(assignment, auditEvent);
        return assignment;
    }

    @Transactional
    public RoleAssignment revoke(
            AuthenticatedPrincipal actor,
            UUID assignmentId,
            long expectedVersion,
            String sourceReference) {
        if (actor == null || assignmentId == null || expectedVersion < 1) {
            throw new IllegalArgumentException("actor, assignment id, and a positive version are required");
        }
        requireRegistered(actor);
        RoleAssignment current = assignments.findAssignment(assignmentId)
                .orElseThrow(RoleAssignmentNotFoundException::new);
        if (actor.equals(current.target())) {
            throw new IllegalArgumentException("a principal cannot revoke its own role assignment");
        }
        InstitutionalReference reference = new InstitutionalReference(sourceReference);
        AccessAuditEvent auditEvent = new AccessAuditEvent(
                UUID.randomUUID(), assignmentId, AccessAuditAction.REVOKED, actor,
                clock.instant(), reference, expectedVersion, expectedVersion + 1);
        return assignments.revoke(assignmentId, expectedVersion, auditEvent);
    }

    private RegisteredIdentity requireRegistered(AuthenticatedPrincipal principal) {
        return identities.find(principal).orElseThrow(IdentityNotRegisteredException::new);
    }

    private static boolean validityOverlaps(
            LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart, LocalDate secondEnd) {
        return (firstEnd == null || !firstEnd.isBefore(secondStart))
                && (secondEnd == null || !secondEnd.isBefore(firstStart));
    }
}
