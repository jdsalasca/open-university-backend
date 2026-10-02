package co.edu.uptc.universiry.admissions.infrastructure.persistence;

import co.edu.uptc.universiry.admissions.application.AdminAdmissionsCall;
import co.edu.uptc.universiry.admissions.application.AdmissionsCallConflictException;
import co.edu.uptc.universiry.admissions.application.AdmissionsCallNotFoundException;
import co.edu.uptc.universiry.admissions.application.AdmissionsCallRepository;
import co.edu.uptc.universiry.admissions.application.PublicAdmissionsCall;
import co.edu.uptc.universiry.admissions.domain.AdmissionsActor;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallRevision;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallStatus;
import co.edu.uptc.universiry.admissions.domain.AdmissionsMilestone;
import co.edu.uptc.universiry.admissions.domain.AdmissionsMilestoneKind;
import co.edu.uptc.universiry.admissions.domain.AdmissionsSource;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAdmissionsCallRepositoryAdapter implements AdmissionsCallRepository {

    private static final String REVISION_SELECT = """
            SELECT r.revision_id, r.call_id, r.revision_number, r.draft_version, r.status,
                   r.title, r.call_name, r.updated_at, r.checked_at, r.source_label, r.source_url,
                   r.confirmation_source_label, r.confirmation_source_url,
                   r.created_by_user_id, r.created_by_identity_id, r.created_at,
                   r.published_by_user_id, r.published_by_identity_id, r.published_at, r.official_reference,
                   m.milestone_key, m.milestone_kind, m.starts_on, m.ends_on, m.title AS milestone_title,
                   m.description AS milestone_description
            FROM admissions_call_revision r
            LEFT JOIN admissions_call_milestone m ON m.revision_id = r.revision_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdmissionsCallRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PublicAdmissionsCall> findPublicCalls() {
        String sql = """
                WITH current_public_calls AS (
                    SELECT c.call_id, c.call_key, c.current_published_revision_id, r.published_at
                    FROM admissions_call c
                    JOIN admissions_call_revision r
                      ON r.call_id = c.call_id AND r.revision_id = c.current_published_revision_id
                         AND r.status = 'PUBLISHED'
                    ORDER BY r.published_at DESC, c.call_key
                    LIMIT 100
                )
                SELECT c.call_id, c.call_key, r.revision_id, r.revision_number,
                       r.title, r.call_name, r.updated_at, r.checked_at, r.source_label, r.source_url,
                       r.confirmation_source_label, r.confirmation_source_url,
                       r.official_reference, r.published_at,
                       m.milestone_key, m.milestone_kind, m.starts_on, m.ends_on,
                       m.title AS milestone_title, m.description AS milestone_description
                FROM current_public_calls c
                JOIN admissions_call_revision r
                  ON r.call_id = c.call_id AND r.revision_id = c.current_published_revision_id
                LEFT JOIN admissions_call_milestone m ON m.revision_id = r.revision_id
                ORDER BY c.published_at DESC, c.call_key, m.starts_on, m.milestone_key
                """;
        Map<UUID, PublicAccumulator> calls = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            UUID callId = uuid(rs, "call_id");
            PublicAccumulator call = calls.computeIfAbsent(callId, ignored -> new PublicAccumulator(rs));
            AdmissionsMilestone milestone = milestone(rs);
            if (milestone != null) call.milestones.add(milestone);
        });
        return calls.values().stream().map(PublicAccumulator::toView).toList();
    }

    @Override
    public List<AdminAdmissionsCall> findAdminCalls() {
        return findAdminCalls("", new Object[0]);
    }

    @Override
    public Optional<AdminAdmissionsCall> findAdminCall(UUID callId) {
        if (callId == null) return Optional.empty();
        return findAdminCalls(" WHERE c.call_id = ?", new Object[]{callId.toString()}).stream().findFirst();
    }

    @Override
    @Transactional
    public void createCall(UUID callId, String callKey, UUID revisionId, AdmissionsCallContent content,
                           AdmissionsActor actor, Instant occurredAt) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO admissions_call (
                        call_id, call_key, current_published_revision_id,
                        created_by_user_id, created_by_identity_id, created_at
                    ) VALUES (?, ?, NULL, ?, ?, ?)
                    """, callId.toString(), callKey, actor.userId().toString(), actor.identityId().toString(),
                    Timestamp.from(occurredAt));
            insertRevision(callId, revisionId, 1, content, actor, occurredAt);
        } catch (DuplicateKeyException duplicate) {
            throw new AdmissionsCallConflictException();
        }
        audit(callId, revisionId, "CALL_CREATED", actor, occurredAt, null, "Borrador de convocatoria creado.");
    }

    @Override
    @Transactional
    public AdmissionsCallRevision createRevision(UUID callId, UUID revisionId, AdmissionsCallContent content,
                                                 AdmissionsActor actor, Instant occurredAt) {
        lockCall(callId);
        Integer nextNumber = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(revision_number), 0) + 1
                FROM admissions_call_revision WHERE call_id = ?
                """, Integer.class, callId.toString());
        int revisionNumber = nextNumber == null ? 1 : nextNumber;
        try {
            insertRevision(callId, revisionId, revisionNumber, content, actor, occurredAt);
        } catch (DuplicateKeyException duplicate) {
            throw new AdmissionsCallConflictException();
        }
        audit(callId, revisionId, "REVISION_CREATED", actor, occurredAt, null, "Nueva revisión de convocatoria creada.");
        return loadRevision(callId, revisionId).orElseThrow(AdmissionsCallNotFoundException::new);
    }

    @Override
    @Transactional
    public AdmissionsCallRevision updateDraft(UUID callId, UUID revisionId, int expectedDraftVersion,
                                              AdmissionsCallContent content, AdmissionsActor actor, Instant occurredAt) {
        lockCall(callId);
        AdmissionsCallRevision current = loadRevision(callId, revisionId)
                .orElseThrow(AdmissionsCallNotFoundException::new);
        if (current.status() != AdmissionsCallStatus.DRAFT || current.draftVersion() != expectedDraftVersion) {
            throw new AdmissionsCallConflictException();
        }
        AdmissionsCallRevision updated = current.revise(content);
        int changed = jdbcTemplate.update("""
                UPDATE admissions_call_revision
                SET draft_version = ?, title = ?, call_name = ?, updated_at = ?, checked_at = ?,
                    source_label = ?, source_url = ?, confirmation_source_label = ?, confirmation_source_url = ?
                WHERE call_id = ? AND revision_id = ? AND status = 'DRAFT' AND draft_version = ?
                """, updated.draftVersion(), content.title(), content.callName(), content.updatedAt(), content.checkedAt(),
                content.source().label(), content.source().url(), content.confirmationSource().label(),
                content.confirmationSource().url(), callId.toString(), revisionId.toString(), expectedDraftVersion);
        if (changed != 1) throw new AdmissionsCallConflictException();
        jdbcTemplate.update("DELETE FROM admissions_call_milestone WHERE revision_id = ?", revisionId.toString());
        insertMilestones(revisionId, content.milestones());
        audit(callId, revisionId, "REVISION_UPDATED", actor, occurredAt, null, "Borrador de convocatoria actualizado.");
        return loadRevision(callId, revisionId).orElseThrow(AdmissionsCallNotFoundException::new);
    }

    @Override
    @Transactional
    public AdmissionsCallRevision publish(UUID callId, UUID revisionId, int expectedDraftVersion,
                                          UUID expectedPublishedRevisionId, String officialReference,
                                          AdmissionsActor actor, Instant occurredAt) {
        UUID currentPublishedRevisionId = lockCall(callId);
        if (!Objects.equals(currentPublishedRevisionId, expectedPublishedRevisionId)) {
            throw new AdmissionsCallConflictException();
        }
        AdmissionsCallRevision draft = loadRevision(callId, revisionId)
                .orElseThrow(AdmissionsCallNotFoundException::new);
        if (draft.draftVersion() != expectedDraftVersion) throw new AdmissionsCallConflictException();
        AdmissionsCallRevision published = draft.publish(actor, occurredAt, officialReference);
        int changed = jdbcTemplate.update("""
                UPDATE admissions_call_revision
                SET status = 'PUBLISHED', published_by_user_id = ?, published_by_identity_id = ?,
                    published_at = ?, official_reference = ?
                WHERE call_id = ? AND revision_id = ? AND status = 'DRAFT' AND draft_version = ?
                """, actor.userId().toString(), actor.identityId().toString(), Timestamp.from(occurredAt),
                published.officialReference(), callId.toString(), revisionId.toString(), expectedDraftVersion);
        if (changed != 1) throw new AdmissionsCallConflictException();
        jdbcTemplate.update("UPDATE admissions_call SET current_published_revision_id = ? WHERE call_id = ?",
                revisionId.toString(), callId.toString());
        audit(callId, revisionId, "REVISION_PUBLISHED", actor, occurredAt, published.officialReference(),
                "Revisión de convocatoria publicada.");
        return loadRevision(callId, revisionId).orElseThrow(AdmissionsCallNotFoundException::new);
    }

    private List<AdminAdmissionsCall> findAdminCalls(String whereClause, Object[] whereArguments) {
        String sql = """
                SELECT c.call_id, c.call_key, c.current_published_revision_id, latest.revision_id AS latest_revision_id
                FROM admissions_call c
                LEFT JOIN admissions_call_revision latest ON latest.call_id = c.call_id
                  AND latest.revision_number = (
                    SELECT MAX(r.revision_number) FROM admissions_call_revision r WHERE r.call_id = c.call_id
                  )
                """ + whereClause + " ORDER BY c.call_key LIMIT 100";
        List<CallHeader> headers = jdbcTemplate.query(sql, (rs, row) -> new CallHeader(
                uuid(rs, "call_id"), rs.getString("call_key"), uuid(rs, "current_published_revision_id"),
                uuid(rs, "latest_revision_id")), whereArguments);
        List<String> revisionIds = headers.stream()
                .flatMap(header -> java.util.stream.Stream.of(header.latestRevisionId(), header.publishedRevisionId()))
                .filter(Objects::nonNull).map(UUID::toString).distinct().toList();
        Map<UUID, AdmissionsCallRevision> revisions = loadRevisions(revisionIds);
        return headers.stream().map(header -> new AdminAdmissionsCall(
                header.callId(), header.callKey(), header.publishedRevisionId(),
                revisions.get(header.latestRevisionId()), revisions.get(header.publishedRevisionId()))).toList();
    }

    private Map<UUID, AdmissionsCallRevision> loadRevisions(List<String> revisionIds) {
        if (revisionIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(revisionIds.size(), "?"));
        Map<UUID, RevisionAccumulator> accumulated = new LinkedHashMap<>();
        jdbcTemplate.query(REVISION_SELECT + " WHERE r.revision_id IN (" + placeholders
                        + ") ORDER BY r.revision_number, m.starts_on, m.milestone_key",
                (rs, row) -> {
                    UUID revisionId = uuid(rs, "revision_id");
                    RevisionAccumulator revision = accumulated.computeIfAbsent(revisionId,
                            ignored -> new RevisionAccumulator(rs));
                    AdmissionsMilestone milestone = milestone(rs);
                    if (milestone != null) revision.milestones.add(milestone);
                    return null;
                }, revisionIds.toArray());
        Map<UUID, AdmissionsCallRevision> result = new LinkedHashMap<>();
        accumulated.forEach((id, builder) -> result.put(id, builder.toRevision()));
        return result;
    }

    private Optional<AdmissionsCallRevision> loadRevision(UUID callId, UUID revisionId) {
        if (callId == null || revisionId == null) return Optional.empty();
        List<AdmissionsCallRevision> revisions = loadRevisions(List.of(revisionId.toString())).values().stream()
                .filter(revision -> revision.callId().equals(callId)).toList();
        return revisions.stream().findFirst();
    }

    private UUID lockCall(UUID callId) {
        List<String> values = jdbcTemplate.query("""
                SELECT current_published_revision_id FROM admissions_call WHERE call_id = ? FOR UPDATE
                """, (rs, row) -> rs.getString("current_published_revision_id"), callId.toString());
        if (values.isEmpty()) throw new AdmissionsCallNotFoundException();
        return values.getFirst() == null ? null : UUID.fromString(values.getFirst());
    }

    private void insertRevision(UUID callId, UUID revisionId, int revisionNumber,
                                AdmissionsCallContent content, AdmissionsActor actor, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO admissions_call_revision (
                    revision_id, call_id, revision_number, draft_version, status,
                    title, call_name, updated_at, checked_at, source_label, source_url,
                    confirmation_source_label, confirmation_source_url,
                    created_by_user_id, created_by_identity_id, created_at
                ) VALUES (?, ?, ?, 1, 'DRAFT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, revisionId.toString(), callId.toString(), revisionNumber, content.title(), content.callName(),
                content.updatedAt(), content.checkedAt(), content.source().label(), content.source().url(),
                content.confirmationSource().label(), content.confirmationSource().url(), actor.userId().toString(),
                actor.identityId().toString(), Timestamp.from(createdAt));
        insertMilestones(revisionId, content.milestones());
    }

    private void insertMilestones(UUID revisionId, List<AdmissionsMilestone> milestones) {
        for (AdmissionsMilestone milestone : milestones) {
            jdbcTemplate.update("""
                    INSERT INTO admissions_call_milestone (
                        revision_id, milestone_key, milestone_kind, starts_on, ends_on, title, description
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, revisionId.toString(), milestone.key(), milestone.kind().name(), milestone.startsOn(),
                    milestone.endsOn(), milestone.title(), milestone.description());
        }
    }

    private void audit(UUID callId, UUID revisionId, String actionKey, AdmissionsActor actor, Instant occurredAt,
                       String officialReference, String summary) {
        jdbcTemplate.update("""
                INSERT INTO admissions_call_audit_event (
                    call_id, revision_id, action_key, actor_user_id, actor_identity_id,
                    occurred_at, official_reference, event_summary
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, callId.toString(), revisionId.toString(), actionKey, actor.userId().toString(),
                actor.identityId().toString(), Timestamp.from(occurredAt), officialReference, summary);
    }

    private static AdmissionsMilestone milestone(ResultSet rs) throws SQLException {
        String key = rs.getString("milestone_key");
        if (key == null) return null;
        return new AdmissionsMilestone(key, AdmissionsMilestoneKind.valueOf(rs.getString("milestone_kind")),
                localDate(rs, "starts_on"), localDate(rs, "ends_on"), rs.getString("milestone_title"),
                rs.getString("milestone_description"));
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private record CallHeader(UUID callId, String callKey, UUID publishedRevisionId, UUID latestRevisionId) {
    }

    private static final class RevisionAccumulator {
        private final UUID id;
        private final UUID callId;
        private final int number;
        private final int draftVersion;
        private final AdmissionsCallStatus status;
        private final String title;
        private final String callName;
        private final LocalDate updatedAt;
        private final LocalDate checkedAt;
        private final AdmissionsSource source;
        private final AdmissionsSource confirmationSource;
        private final UUID createdByUserId;
        private final UUID createdByIdentityId;
        private final Instant createdAt;
        private final UUID publishedByUserId;
        private final UUID publishedByIdentityId;
        private final Instant publishedAt;
        private final String officialReference;
        private final List<AdmissionsMilestone> milestones = new ArrayList<>();

        private RevisionAccumulator(ResultSet rs) {
            try {
                id = uuid(rs, "revision_id");
                callId = uuid(rs, "call_id");
                number = rs.getInt("revision_number");
                draftVersion = rs.getInt("draft_version");
                status = AdmissionsCallStatus.valueOf(rs.getString("status"));
                title = rs.getString("title");
                callName = rs.getString("call_name");
                updatedAt = localDate(rs, "updated_at");
                checkedAt = localDate(rs, "checked_at");
                source = new AdmissionsSource(rs.getString("source_label"), rs.getString("source_url"));
                confirmationSource = new AdmissionsSource(rs.getString("confirmation_source_label"),
                        rs.getString("confirmation_source_url"));
                createdByUserId = uuid(rs, "created_by_user_id");
                createdByIdentityId = uuid(rs, "created_by_identity_id");
                createdAt = instant(rs, "created_at");
                publishedByUserId = uuid(rs, "published_by_user_id");
                publishedByIdentityId = uuid(rs, "published_by_identity_id");
                publishedAt = instant(rs, "published_at");
                officialReference = rs.getString("official_reference");
            } catch (SQLException exception) {
                throw new org.springframework.jdbc.UncategorizedSQLException("map admissions revision", "", exception);
            }
        }

        private AdmissionsCallRevision toRevision() {
            return new AdmissionsCallRevision(id, callId, number, draftVersion, status,
                    new AdmissionsCallContent(title, callName, updatedAt, checkedAt, source, confirmationSource, milestones),
                    createdByUserId, createdByIdentityId, createdAt, publishedByUserId, publishedByIdentityId,
                    publishedAt, officialReference);
        }
    }

    private static final class PublicAccumulator {
        private final UUID callId;
        private final String callKey;
        private final UUID revisionId;
        private final int revisionNumber;
        private final String title;
        private final String callName;
        private final LocalDate updatedAt;
        private final LocalDate checkedAt;
        private final AdmissionsSource source;
        private final AdmissionsSource confirmationSource;
        private final String officialReference;
        private final Instant publishedAt;
        private final List<AdmissionsMilestone> milestones = new ArrayList<>();

        private PublicAccumulator(ResultSet rs) {
            try {
                callId = uuid(rs, "call_id");
                callKey = rs.getString("call_key");
                revisionId = uuid(rs, "revision_id");
                revisionNumber = rs.getInt("revision_number");
                title = rs.getString("title");
                callName = rs.getString("call_name");
                updatedAt = localDate(rs, "updated_at");
                checkedAt = localDate(rs, "checked_at");
                source = new AdmissionsSource(rs.getString("source_label"), rs.getString("source_url"));
                confirmationSource = new AdmissionsSource(rs.getString("confirmation_source_label"),
                        rs.getString("confirmation_source_url"));
                officialReference = rs.getString("official_reference");
                publishedAt = instant(rs, "published_at");
            } catch (SQLException exception) {
                throw new org.springframework.jdbc.UncategorizedSQLException("map public admissions call", "", exception);
            }
        }

        private PublicAdmissionsCall toView() {
            return new PublicAdmissionsCall(callId, callKey, revisionId, revisionNumber,
                    new AdmissionsCallContent(title, callName, updatedAt, checkedAt, source, confirmationSource, milestones),
                    officialReference, publishedAt);
        }
    }
}
