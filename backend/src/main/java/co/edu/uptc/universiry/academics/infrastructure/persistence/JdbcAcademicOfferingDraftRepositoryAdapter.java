package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicOfferingAuditCursor;
import co.edu.uptc.universiry.academics.application.AcademicOfferingAuditPage;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftAuditAction;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftAuditEvent;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftCursor;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftNotFoundException;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftPage;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftRepository;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftSnapshot;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftView;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftConflictException;
import co.edu.uptc.universiry.academics.application.AcademicOfferingReferenceNotFoundException;
import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;
import co.edu.uptc.universiry.academics.domain.AcademicOfferingVersionConflictException;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAcademicOfferingDraftRepositoryAdapter implements AcademicOfferingDraftRepository {

    private static final String DRAFT_SELECT = """
            SELECT d.offering_id, d.period_id, d.curriculum_id, d.subject_id, d.section_code,
                   d.starts_on, d.ends_on, d.proposed_capacity, d.version, d.source_reference,
                   d.created_at, d.updated_at, d.updated_by, p.period_code, p.period_kind,
                   c.curriculum_version, pr.program_code, prv.program_name, s.subject_code, sr.subject_name
            FROM academic_offering_draft d
            JOIN academic_period p ON p.period_id = d.period_id
            JOIN academic_curriculum c ON c.curriculum_id = d.curriculum_id AND c.status = 'PUBLISHED'
            JOIN academic_program pr ON pr.program_id = c.program_id
            JOIN academic_program_revision prv
              ON prv.program_revision_id = c.program_revision_id AND prv.program_id = c.program_id
            JOIN academic_curriculum_entry e
              ON e.curriculum_id = d.curriculum_id AND e.subject_id = d.subject_id
            JOIN academic_subject s ON s.subject_id = e.subject_id
            JOIN academic_subject_revision sr
              ON sr.subject_revision_id = e.subject_revision_id AND sr.subject_id = e.subject_id
            """;

    private static final RowMapper<AcademicOfferingDraftView> VIEW_MAPPER = JdbcAcademicOfferingDraftRepositoryAdapter::mapView;
    private static final RowMapper<AcademicOfferingDraft> DRAFT_MAPPER = JdbcAcademicOfferingDraftRepositoryAdapter::mapDraft;
    private static final RowMapper<AcademicOfferingDraftAuditEvent> EVENT_MAPPER =
            JdbcAcademicOfferingDraftRepositoryAdapter::mapEvent;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAcademicOfferingDraftRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicOfferingDraftPage findByPeriod(UUID periodId, int limit, AcademicOfferingDraftCursor before) {
        String cursorFilter = before == null ? "" : " AND (d.created_at < ? OR "
                + "(d.created_at = ? AND d.offering_id < ?))";
        var parameters = new java.util.ArrayList<Object>();
        parameters.add(periodId.toString());
        if (before != null) {
            Timestamp cursorTime = Timestamp.from(before.createdAt());
            parameters.add(cursorTime);
            parameters.add(cursorTime);
            parameters.add(before.id().toString());
        }
        parameters.add(limit + 1);
        List<AcademicOfferingDraftView> rows = jdbcTemplate.query(
                DRAFT_SELECT + " WHERE d.period_id = ?" + cursorFilter
                        + " ORDER BY d.created_at DESC, d.offering_id DESC LIMIT ?",
                VIEW_MAPPER, parameters.toArray());
        boolean hasMore = rows.size() > limit;
        List<AcademicOfferingDraftView> page = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore
                ? new AcademicOfferingDraftCursor(page.getLast().createdAt(), page.getLast().draft().id()).encode()
                : null;
        return new AcademicOfferingDraftPage(page, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicOfferingDraft> find(UUID id) {
        if (id == null) return Optional.empty();
        return jdbcTemplate.query("""
                SELECT offering_id, period_id, curriculum_id, subject_id, section_code, starts_on, ends_on,
                       proposed_capacity, version
                FROM academic_offering_draft WHERE offering_id = ?
                """, DRAFT_MAPPER, id.toString()).stream().findFirst();
    }

    @Override
    @Transactional
    public void create(AcademicOfferingDraft draft, String reference, String actor, Instant at) {
        requireReferences(draft.periodId(), draft.curriculumId(), draft.subjectId());
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_offering_draft (
                        offering_id, period_id, curriculum_id, subject_id, section_code, starts_on, ends_on,
                        proposed_capacity, source_reference, version, created_by, created_at, updated_by, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, draft.id().toString(), draft.periodId().toString(), draft.curriculumId().toString(),
                    draft.subjectId().toString(), draft.sectionCode(), draft.startsOn(), draft.endsOn(),
                    draft.proposedCapacity(), reference, draft.version(), actor, timestamp(at), actor, timestamp(at));
        } catch (DuplicateKeyException conflict) {
            throw new AcademicOfferingDraftConflictException();
        }
        insertEvent(draft.id(), AcademicOfferingDraftAuditAction.OFFERING_DRAFT_CREATED, actor, at,
                reference, null, AcademicOfferingDraftSnapshot.from(draft));
    }

    @Override
    @Transactional
    public void update(AcademicOfferingDraft draft, int expectedVersion, String reference, String actor, Instant at) {
        AcademicOfferingDraft before = jdbcTemplate.query("""
                SELECT offering_id, period_id, curriculum_id, subject_id, section_code, starts_on, ends_on,
                       proposed_capacity, version
                FROM academic_offering_draft WHERE offering_id = ? FOR UPDATE
                """, DRAFT_MAPPER, draft.id().toString()).stream().findFirst()
                .orElseThrow(AcademicOfferingDraftNotFoundException::new);
        if (before.version() != expectedVersion) throw new AcademicOfferingVersionConflictException();
        int changed = jdbcTemplate.update("""
                UPDATE academic_offering_draft
                SET section_code = ?, starts_on = ?, ends_on = ?, proposed_capacity = ?, source_reference = ?,
                    version = ?, updated_by = ?, updated_at = ?
                WHERE offering_id = ? AND version = ?
                """, draft.sectionCode(), draft.startsOn(), draft.endsOn(), draft.proposedCapacity(), reference,
                draft.version(), actor, timestamp(at), draft.id().toString(), expectedVersion);
        if (changed != 1) throw new AcademicOfferingVersionConflictException();
        insertEvent(draft.id(), AcademicOfferingDraftAuditAction.OFFERING_DRAFT_UPDATED, actor, at,
                reference, AcademicOfferingDraftSnapshot.from(before), AcademicOfferingDraftSnapshot.from(draft));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicOfferingAuditPage findAuditEvents(UUID offeringId, int limit, Long beforeId) {
        String cursorFilter = beforeId == null ? "" : " AND audit_event_id < ?";
        var parameters = new java.util.ArrayList<Object>();
        parameters.add(offeringId.toString());
        if (beforeId != null) parameters.add(beforeId);
        parameters.add(limit + 1);
        List<AcademicOfferingDraftAuditEvent> rows = jdbcTemplate.query("""
                SELECT audit_event_id, offering_id, action_key, actor_sub, occurred_at, source_reference,
                       before_section_code, before_starts_on, before_ends_on, before_proposed_capacity,
                       before_version, after_section_code, after_starts_on, after_ends_on,
                       after_proposed_capacity, after_version
                FROM academic_offering_draft_audit_event
                WHERE offering_id = ?
                """ + cursorFilter + " ORDER BY audit_event_id DESC LIMIT ?", EVENT_MAPPER, parameters.toArray());
        boolean hasMore = rows.size() > limit;
        List<AcademicOfferingDraftAuditEvent> page = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore ? new AcademicOfferingAuditCursor(page.getLast().id()).encode() : null;
        return new AcademicOfferingAuditPage(page, nextCursor);
    }

    private void requireReferences(UUID periodId, UUID curriculumId, UUID subjectId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM academic_period p
                JOIN academic_curriculum c ON c.curriculum_id = ? AND c.status = 'PUBLISHED'
                JOIN academic_curriculum_entry e
                  ON e.curriculum_id = c.curriculum_id AND e.subject_id = ?
                WHERE p.period_id = ?
                """, Integer.class, curriculumId.toString(), subjectId.toString(), periodId.toString());
        if (count == null || count != 1) throw new AcademicOfferingReferenceNotFoundException();
    }

    private void insertEvent(UUID id, AcademicOfferingDraftAuditAction action, String actor, Instant at,
                             String reference, AcademicOfferingDraftSnapshot before,
                             AcademicOfferingDraftSnapshot after) {
        jdbcTemplate.update("""
                INSERT INTO academic_offering_draft_audit_event (
                    offering_id, action_key, actor_sub, occurred_at, source_reference,
                    before_section_code, before_starts_on, before_ends_on, before_proposed_capacity, before_version,
                    after_section_code, after_starts_on, after_ends_on, after_proposed_capacity, after_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id.toString(), action.name(), actor, timestamp(at), reference,
                before == null ? null : before.sectionCode(),
                before == null ? null : before.startsOn(),
                before == null ? null : before.endsOn(),
                before == null ? null : before.proposedCapacity(),
                before == null ? null : before.version(),
                after.sectionCode(), after.startsOn(), after.endsOn(), after.proposedCapacity(), after.version());
    }

    private static AcademicOfferingDraftView mapView(ResultSet rs, int row) throws SQLException {
        AcademicOfferingDraft draft = mapDraft(rs, row);
        return new AcademicOfferingDraftView(draft, rs.getString("period_code"),
                AcademicPeriodKind.valueOf(rs.getString("period_kind")), rs.getString("program_code"),
                rs.getString("program_name"), rs.getString("curriculum_version"), rs.getString("subject_code"),
                rs.getString("subject_name"), rs.getString("source_reference"), rs.getString("updated_by"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private static AcademicOfferingDraft mapDraft(ResultSet rs, int row) throws SQLException {
        return new AcademicOfferingDraft(uuid(rs.getString("offering_id")), uuid(rs.getString("period_id")),
                uuid(rs.getString("curriculum_id")), uuid(rs.getString("subject_id")),
                rs.getString("section_code"), localDate(rs, "starts_on"), localDate(rs, "ends_on"),
                rs.getInt("proposed_capacity"), rs.getInt("version"));
    }

    private static AcademicOfferingDraftAuditEvent mapEvent(ResultSet rs, int row) throws SQLException {
        return new AcademicOfferingDraftAuditEvent(rs.getLong("audit_event_id"),
                uuid(rs.getString("offering_id")),
                AcademicOfferingDraftAuditAction.valueOf(rs.getString("action_key")),
                rs.getString("actor_sub"), instant(rs, "occurred_at"), rs.getString("source_reference"),
                snapshot(rs, "before"), snapshot(rs, "after"));
    }

    private static AcademicOfferingDraftSnapshot snapshot(ResultSet rs, String prefix) throws SQLException {
        String sectionCode = rs.getString(prefix + "_section_code");
        if (sectionCode == null) return null;
        return new AcademicOfferingDraftSnapshot(sectionCode, localDate(rs, prefix + "_starts_on"),
                localDate(rs, prefix + "_ends_on"), rs.getInt(prefix + "_proposed_capacity"),
                rs.getInt(prefix + "_version"));
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        var value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value.trim());
    }
}
