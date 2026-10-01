package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AccessAuditEvent;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleAssignmentRepository {

    List<RoleAssignment> findAssignments(AuthenticatedPrincipal target);

    Optional<RoleAssignment> findAssignment(UUID assignmentId);

    List<RoleAssignment> findActiveAssignments(AuthenticatedPrincipal target, LocalDate institutionalDate);

    void create(RoleAssignment assignment, AccessAuditEvent auditEvent);

    RoleAssignment revoke(UUID assignmentId, long expectedVersion, AccessAuditEvent auditEvent);
}
