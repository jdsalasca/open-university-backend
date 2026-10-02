package co.edu.uptc.universiry.identity.infrastructure.persistence;

import co.edu.uptc.universiry.identity.application.RoleAssignmentNotFoundException;
import co.edu.uptc.universiry.identity.application.RoleAssignmentRepository;
import co.edu.uptc.universiry.identity.application.RoleAssignmentVersionConflictException;
import co.edu.uptc.universiry.identity.domain.AccessAuditAction;
import co.edu.uptc.universiry.identity.domain.AccessAuditEvent;
import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.AssignmentStatus;
import co.edu.uptc.universiry.identity.domain.InstitutionalReference;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.identity.domain.RoleProfile;
import co.edu.uptc.universiry.identity.domain.ScopeKind;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcRoleAssignmentRepositoryAdapter implements RoleAssignmentRepository {

    private static final RowMapper<AssignmentRow> ASSIGNMENT_ROW_MAPPER = JdbcRoleAssignmentRepositoryAdapter::mapAssignmentRow;

    private final JdbcTemplate jdbcTemplate;
    public JdbcRoleAssignmentRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleAssignment> findAssignments(UUID targetUserId) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("canonical assignment target is required");
        }
        List<AssignmentRow> rows = jdbcTemplate.query("""
                SELECT a.assignment_id, a.profile_key, a.status, a.valid_from, a.valid_through,
                       a.source_reference, a.created_at, a.version, a.target_user_id, a.granted_by_user_id
                FROM identity_role_assignment a
                WHERE a.target_user_id = ?
                ORDER BY a.created_at DESC, a.assignment_id
                """, ASSIGNMENT_ROW_MAPPER, targetUserId.toString());
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, Set<AssignmentScope>> scopes = findScopes(rows.stream().map(AssignmentRow::id).toList());
        return rows.stream().map(row -> row.toDomain(scopes.getOrDefault(row.id(), Set.of()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleAssignment> findAssignment(UUID assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("assignment id is required");
        }
        return findById(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleAssignment> findActiveAssignments(UUID targetUserId, LocalDate institutionalDate) {
        if (targetUserId == null || institutionalDate == null) {
            throw new IllegalArgumentException("canonical assignment target and institutional date are required");
        }
        return findAssignments(targetUserId).stream()
                .filter(assignment -> assignment.isActiveOn(institutionalDate)).toList();
    }

    @Override
    @Transactional
    public void create(RoleAssignment assignment, AccessAuditEvent auditEvent) {
        validateGrantEvent(assignment, auditEvent);
        jdbcTemplate.update("""
                INSERT INTO identity_role_assignment (
                    assignment_id, target_user_id, profile_key, status, valid_from, valid_through,
                    source_reference, granted_by_user_id, created_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, assignment.id().toString(), assignment.targetUserId().toString(), assignment.profile().key(),
                assignment.status().name(), assignment.validFrom(), assignment.validThrough(),
                assignment.sourceReference().value(), assignment.grantedByUserId().toString(),
                Timestamp.from(assignment.createdAt()), assignment.version());
        insertScopes(assignment);
        appendAuditEvent(auditEvent);
    }

    @Override
    @Transactional
    public RoleAssignment revoke(UUID assignmentId, long expectedVersion, AccessAuditEvent auditEvent) {
        validateRevokeEvent(assignmentId, expectedVersion, auditEvent);
        int updated = jdbcTemplate.update("""
                UPDATE identity_role_assignment
                SET status = 'REVOKED', version = version + 1
                WHERE assignment_id = ? AND status = 'ACTIVE' AND version = ?
                """, assignmentId.toString(), expectedVersion);
        if (updated != 1) {
            Integer existing = jdbcTemplate.query(
                    "SELECT version FROM identity_role_assignment WHERE assignment_id = ?",
                    resultSet -> resultSet.next() ? resultSet.getInt(1) : null,
                    assignmentId.toString());
            if (existing == null) {
                throw new RoleAssignmentNotFoundException();
            }
            throw new RoleAssignmentVersionConflictException();
        }
        validateSuccessfulRevokeEvent(expectedVersion, auditEvent);
        appendAuditEvent(auditEvent);
        return findById(assignmentId).orElseThrow(RoleAssignmentNotFoundException::new);
    }

    private Optional<RoleAssignment> findById(UUID assignmentId) {
        List<AssignmentRow> rows = jdbcTemplate.query("""
                SELECT a.assignment_id, a.profile_key, a.status, a.valid_from, a.valid_through,
                       a.source_reference, a.created_at, a.version, a.target_user_id, a.granted_by_user_id
                FROM identity_role_assignment a
                WHERE a.assignment_id = ?
                """, ASSIGNMENT_ROW_MAPPER, assignmentId.toString());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        AssignmentRow row = rows.getFirst();
        return Optional.of(row.toDomain(findScopes(List.of(assignmentId)).getOrDefault(assignmentId, Set.of())));
    }

    private Map<UUID, Set<AssignmentScope>> findScopes(List<UUID> assignmentIds) {
        Map<UUID, Set<AssignmentScope>> result = new HashMap<>();
        assignmentIds.forEach(id -> result.put(id, new HashSet<>()));
        if (assignmentIds.isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", assignmentIds.stream().map(ignored -> "?").toList());
        String sql = """
                SELECT assignment_id, scope_kind, site_id, organization_unit_id, program_id,
                       job_appointment_reference
                FROM identity_role_assignment_scope
                WHERE assignment_id IN (%s)
                ORDER BY assignment_id, scope_kind
                """.formatted(placeholders);
        PreparedStatementSetter setter = statement -> {
            for (int index = 0; index < assignmentIds.size(); index++) {
                statement.setString(index + 1, assignmentIds.get(index).toString());
            }
        };
        jdbcTemplate.query(sql, setter, resultSet -> {
            UUID assignmentId = UUID.fromString(resultSet.getString("assignment_id"));
            ScopeKind kind = ScopeKind.valueOf(resultSet.getString("scope_kind"));
            String reference = switch (kind) {
                case UNIVERSITY -> null;
                case SITE -> resultSet.getString("site_id");
                case FACULTY -> resultSet.getString("organization_unit_id");
                case PROGRAM -> resultSet.getString("program_id");
                case JOB_APPOINTMENT -> resultSet.getString("job_appointment_reference");
            };
            result.get(assignmentId).add(new AssignmentScope(kind, reference));
        });
        return result;
    }

    private void insertScopes(RoleAssignment assignment) {
        for (AssignmentScope scope : assignment.scopes()) {
            String siteId = null;
            String organizationUnitId = null;
            String programId = null;
            String jobAppointmentReference = null;
            switch (scope.kind()) {
                case UNIVERSITY -> {
                }
                case SITE -> siteId = canonicalUuid(scope.stableReference());
                case FACULTY -> organizationUnitId = canonicalUuid(scope.stableReference());
                case PROGRAM -> programId = canonicalUuid(scope.stableReference());
                case JOB_APPOINTMENT -> jobAppointmentReference = scope.stableReference();
            }
            jdbcTemplate.update("""
                    INSERT INTO identity_role_assignment_scope (
                        assignment_id, scope_kind, site_id, organization_unit_id, program_id,
                        job_appointment_reference
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """, assignment.id().toString(), scope.kind().name(), siteId, organizationUnitId,
                    programId, jobAppointmentReference);
        }
    }

    private void appendAuditEvent(AccessAuditEvent event) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO identity_access_audit_event (
                    audit_event_id, assignment_id, action_key, actor_identity_id, actor_user_id, occurred_at,
                    source_reference, previous_version, version
                )
                SELECT ?, ?, ?, identity_binding.identity_id, identity_binding.user_id, ?, ?, ?, ?
                FROM institutional_identity identity_binding
                WHERE identity_binding.identity_id = ? AND identity_binding.user_id = ?
                """, event.id().toString(), event.assignmentId().toString(), event.action().name(),
                Timestamp.from(event.occurredAt()), event.sourceReference().value(), event.previousVersion(),
                event.version(), event.actorIdentityId().toString(), event.actorUserId().toString());
        if (inserted != 1) {
            throw new IllegalArgumentException("audit actor identity must belong to its canonical user");
        }
    }

    private static void validateGrantEvent(RoleAssignment assignment, AccessAuditEvent event) {
        if (assignment == null || event == null || assignment.status() != AssignmentStatus.ACTIVE
                || assignment.version() != 1 || event.action() != AccessAuditAction.GRANTED
                || !event.assignmentId().equals(assignment.id())
                || !event.actorUserId().equals(assignment.grantedByUserId())
                || !event.sourceReference().equals(assignment.sourceReference())
                || event.previousVersion() != 0 || event.version() != assignment.version()) {
            throw new IllegalArgumentException("grant event must describe the initial assignment and its grantor");
        }
    }

    private static void validateRevokeEvent(UUID assignmentId, long expectedVersion, AccessAuditEvent event) {
        if (assignmentId == null || event == null || expectedVersion < 0
                || event.action() != AccessAuditAction.REVOKED || !event.assignmentId().equals(assignmentId)) {
            throw new IllegalArgumentException("revocation event must identify the requested assignment");
        }
    }

    private static void validateSuccessfulRevokeEvent(long expectedVersion, AccessAuditEvent event) {
        if (event.previousVersion() != expectedVersion || event.version() != expectedVersion + 1) {
            throw new IllegalArgumentException("revocation event must describe the committed version transition");
        }
    }

    private static String canonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("academic scope reference must be a stable UUID", exception);
        }
    }

    private static AssignmentRow mapAssignmentRow(ResultSet resultSet, int rowNumber) throws SQLException {
        Date through = resultSet.getDate("valid_through");
        return new AssignmentRow(
                UUID.fromString(resultSet.getString("assignment_id")),
                UUID.fromString(resultSet.getString("target_user_id")),
                UUID.fromString(resultSet.getString("granted_by_user_id")),
                RoleProfile.fromKey(resultSet.getString("profile_key")),
                AssignmentStatus.valueOf(resultSet.getString("status")),
                resultSet.getDate("valid_from").toLocalDate(),
                through == null ? null : through.toLocalDate(),
                new InstitutionalReference(resultSet.getString("source_reference")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getLong("version"));
    }

    private record AssignmentRow(
            UUID id,
            UUID targetUserId,
            UUID grantedByUserId,
            RoleProfile profile,
            AssignmentStatus status,
            LocalDate validFrom,
            LocalDate validThrough,
            InstitutionalReference sourceReference,
            java.time.Instant createdAt,
            long version) {

        private RoleAssignment toDomain(Set<AssignmentScope> scopes) {
            return new RoleAssignment(id, targetUserId, profile, scopes, validFrom, validThrough, status,
                    grantedByUserId, sourceReference, createdAt, version);
        }
    }
}
