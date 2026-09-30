package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicPeriodConflictException;
import co.edu.uptc.universiry.academics.application.AcademicPeriodRepository;
import co.edu.uptc.universiry.academics.application.AcademicPeriodView;
import co.edu.uptc.universiry.academics.application.AcademicPeriodNotFoundException;
import co.edu.uptc.universiry.academics.application.AcademicPeriodAuditEvent;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarActivity;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarRevision;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarStatus;
import co.edu.uptc.universiry.academics.domain.AcademicPeriod;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAcademicPeriodRepositoryAdapter implements AcademicPeriodRepository {

    private static final ZoneId INSTITUTION_ZONE = ZoneId.of("America/Bogota");

    private static final String VIEW_SELECT = """
            SELECT p.period_id, p.period_code, p.period_kind, p.academic_year, p.sequence_number,
                   p.starts_on, p.ends_on, p.status, p.approved_calendar_revision_id,
                   p.approval_reference, p.approved_by, p.approved_at, p.opened_by, p.opened_at,
                   p.closed_by, p.closed_at,
                   p.cancelled_by, p.cancelled_at, p.created_at,
                   c.revision_number AS calendar_revision_number, c.official_reference
            FROM academic_period p
            LEFT JOIN academic_calendar_revision c
              ON c.calendar_revision_id = p.approved_calendar_revision_id AND c.period_id = p.period_id
            """;

    private static final RowMapper<AcademicPeriod> PERIOD_MAPPER = (rs, row) -> mapPeriod(rs);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcAcademicPeriodRepositoryAdapter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public List<AcademicPeriodView> findPublicPeriods() {
        return jdbcTemplate.query(VIEW_SELECT + " WHERE p.status = 'OPEN' ORDER BY p.starts_on, p.period_kind, p.period_code",
                this::mapView);
    }

    @Override
    public List<AcademicPeriodView> findAdminPeriods() {
        return jdbcTemplate.query(VIEW_SELECT + " ORDER BY p.starts_on DESC, p.period_code LIMIT 100",
                this::mapView);
    }

    @Override
    public Optional<AcademicPeriod> findPeriod(UUID periodId) {
        return jdbcTemplate.query("SELECT * FROM academic_period WHERE period_id = ?",
                PERIOD_MAPPER, periodId.toString()).stream().findFirst();
    }

    @Override
    public Optional<AcademicPeriodView> findView(UUID periodId) {
        return jdbcTemplate.query(VIEW_SELECT + " WHERE p.period_id = ?", this::mapView, periodId.toString())
                .stream().findFirst();
    }

    @Override
    public Optional<AcademicCalendarRevision> findCalendar(UUID periodId, UUID revisionId) {
        List<CalendarHeader> headers = jdbcTemplate.query("""
                SELECT calendar_revision_id, period_id, revision_number, official_reference, status,
                       published_by, published_at
                FROM academic_calendar_revision
                WHERE period_id = ? AND calendar_revision_id = ?
                """, (rs, row) -> new CalendarHeader(
                uuid(rs.getString("calendar_revision_id")), uuid(rs.getString("period_id")),
                rs.getInt("revision_number"), rs.getString("official_reference"),
                AcademicCalendarStatus.valueOf(rs.getString("status")), rs.getString("published_by"),
                instant(rs, "published_at")), periodId.toString(), revisionId.toString());
        if (headers.isEmpty()) return Optional.empty();
        CalendarHeader header = headers.getFirst();
        List<AcademicCalendarActivity> activities = jdbcTemplate.query("""
                SELECT activity_key, display_name, starts_at, ends_at, organization_unit_id, site_id
                FROM academic_calendar_activity
                WHERE calendar_revision_id = ?
                ORDER BY starts_at, activity_key
                """, (rs, row) -> new AcademicCalendarActivity(
                rs.getString("activity_key"), rs.getString("display_name"), localDateTime(rs, "starts_at"),
                localDateTime(rs, "ends_at"), nullableUuid(rs.getString("organization_unit_id")),
                nullableUuid(rs.getString("site_id"))), revisionId.toString());
        AcademicCalendarRevision revision = AcademicCalendarRevision.restore(
                header.id(), header.periodId(), header.version(), header.reference(), header.status(),
                activities, header.publishedBy(), header.publishedAt());
        return Optional.of(revision);
    }

    @Override
    public List<AcademicCalendarRevision> findCalendarHistory(UUID periodId) {
        List<CalendarHistoryRow> rows = jdbcTemplate.query("""
                SELECT c.calendar_revision_id, c.period_id, c.revision_number, c.official_reference, c.status,
                       c.published_by, c.published_at, a.activity_key, a.display_name, a.starts_at, a.ends_at,
                       a.organization_unit_id, a.site_id
                FROM academic_calendar_revision c
                LEFT JOIN academic_calendar_activity a ON a.calendar_revision_id = c.calendar_revision_id
                WHERE c.period_id = ?
                ORDER BY c.revision_number, a.starts_at, a.activity_key
                """, (rs, row) -> new CalendarHistoryRow(
                new CalendarHeader(uuid(rs.getString("calendar_revision_id")), uuid(rs.getString("period_id")),
                        rs.getInt("revision_number"), rs.getString("official_reference"),
                        AcademicCalendarStatus.valueOf(rs.getString("status")), rs.getString("published_by"),
                        instant(rs, "published_at")),
                rs.getString("activity_key") == null ? null : new AcademicCalendarActivity(
                        rs.getString("activity_key"), rs.getString("display_name"),
                        localDateTime(rs, "starts_at"), localDateTime(rs, "ends_at"),
                        nullableUuid(rs.getString("organization_unit_id")),
                        nullableUuid(rs.getString("site_id")))), periodId.toString());
        java.util.Map<UUID, CalendarHistoryBuilder> histories = new java.util.LinkedHashMap<>();
        for (CalendarHistoryRow row : rows) {
            CalendarHistoryBuilder history = histories.computeIfAbsent(row.header().id(),
                    ignored -> new CalendarHistoryBuilder(row.header()));
            if (row.activity() != null) history.activities().add(row.activity());
        }
        return histories.values().stream().map(history -> AcademicCalendarRevision.restore(
                history.header().id(), history.header().periodId(), history.header().version(),
                history.header().reference(), history.header().status(), history.activities(),
                history.header().publishedBy(), history.header().publishedAt())).toList();
    }

    @Override
    public List<AcademicPeriodAuditEvent> findAuditHistory(UUID periodId) {
        return jdbcTemplate.query("""
                SELECT audit_event_id, action_key, actor_sub, occurred_at, reference, event_summary
                FROM academic_period_audit_event
                WHERE period_id = ? ORDER BY audit_event_id
                """, (rs, row) -> new AcademicPeriodAuditEvent(rs.getLong("audit_event_id"),
                rs.getString("action_key"), rs.getString("actor_sub"), instant(rs, "occurred_at"),
                rs.getString("reference"), rs.getString("event_summary")), periodId.toString());
    }

    @Override
    public int nextCalendarRevisionNumber(UUID periodId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(revision_number), 0) + 1 FROM academic_calendar_revision WHERE period_id = ?",
                Integer.class, periodId.toString());
        return value == null ? 1 : value;
    }

    @Override
    @Transactional
    public void createPeriod(AcademicPeriod period, String actorSub) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_period (
                        period_id, period_code, period_kind, academic_year, sequence_number,
                        starts_on, ends_on, status, created_by, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?)
                    """, period.id().toString(), period.code(), period.kind().name(), period.academicYear(),
                    period.sequenceNumber(), period.startsOn(), period.endsOn(), actorSub, nowTimestamp());
        } catch (DuplicateKeyException duplicate) {
            throw new AcademicPeriodConflictException();
        }
        audit(period.id(), "PERIOD_CREATED", actorSub, null, "Academic period created.");
    }

    @Override
    @Transactional
    public void createCalendar(AcademicCalendarRevision revision, String actorSub) {
        AcademicPeriodStatus status = lockPeriodStatus(revision.periodId());
        requireEditableCalendarState(status);
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_calendar_revision (
                        calendar_revision_id, period_id, revision_number, official_reference,
                        status, created_by, created_at
                    ) VALUES (?, ?, ?, ?, 'DRAFT', ?, ?)
                    """, revision.id().toString(), revision.periodId().toString(), revision.version(),
                    revision.officialReference(), actorSub, nowTimestamp());
        } catch (DuplicateKeyException duplicate) {
            throw new AcademicPeriodConflictException();
        }
        for (AcademicCalendarActivity activity : revision.activities()) {
            jdbcTemplate.update("""
                    INSERT INTO academic_calendar_activity (
                        activity_id, calendar_revision_id, activity_key, display_name, starts_at, ends_at,
                        organization_unit_id, site_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), revision.id().toString(), activity.key(), activity.label(),
                    Timestamp.from(activity.startsAt().atZone(INSTITUTION_ZONE).toInstant()),
                    Timestamp.from(activity.endsAt().atZone(INSTITUTION_ZONE).toInstant()),
                    idString(activity.organizationUnitId()), idString(activity.siteId()));
        }
        audit(revision.periodId(), "CALENDAR_CREATED", actorSub, revision.officialReference(),
                "Academic calendar draft created.");
    }

    @Override
    @Transactional
    public void publishCalendar(AcademicCalendarRevision revision, String actorSub) {
        AcademicPeriodStatus status = lockPeriodStatus(revision.periodId());
        requireEditableCalendarState(status);
        int changed = jdbcTemplate.update("""
                UPDATE academic_calendar_revision
                SET status = 'PUBLISHED', published_by = ?, published_at = ?
                WHERE calendar_revision_id = ? AND period_id = ? AND status = 'DRAFT'
                """, actorSub, nowTimestamp(), revision.id().toString(), revision.periodId().toString());
        if (changed != 1) throw new AcademicPeriodConflictException();
        audit(revision.periodId(), "CALENDAR_PUBLISHED", actorSub, revision.officialReference(),
                "Academic calendar published.");
    }

    @Override
    @Transactional
    public void selectCalendarRevision(AcademicPeriod period, UUID expectedRevisionId, UUID selectedRevisionId,
                                       String actorSub, String officialReference) {
        AcademicPeriodStatus currentStatus = lockPeriodStatus(period.id());
        if (currentStatus != period.status()) throw new AcademicPeriodConflictException();
        int changed = jdbcTemplate.update("""
                UPDATE academic_period
                SET approved_calendar_revision_id = ?
                WHERE period_id = ? AND status = ? AND approved_calendar_revision_id = ?
                  AND EXISTS (
                    SELECT 1 FROM academic_calendar_revision selected
                    WHERE selected.calendar_revision_id = ? AND selected.period_id = ?
                      AND selected.status = 'PUBLISHED'
                      AND selected.revision_number > (
                        SELECT active.revision_number FROM academic_calendar_revision active
                        WHERE active.calendar_revision_id = academic_period.approved_calendar_revision_id
                          AND active.period_id = academic_period.period_id
                      )
                      AND selected.revision_number = (
                        SELECT MAX(latest.revision_number) FROM academic_calendar_revision latest
                        WHERE latest.period_id = academic_period.period_id AND latest.status = 'PUBLISHED'
                      )
                  )
                """, period.approvedCalendarRevisionId().toString(), period.id().toString(),
                period.status().name(), expectedRevisionId.toString(), selectedRevisionId.toString(),
                period.id().toString());
        if (changed != 1) throw new AcademicPeriodConflictException();
        audit(period.id(), "PERIOD_CALENDAR_AMENDED", actorSub, officialReference,
                "Selected academic calendar revision " + period.approvedCalendarRevisionId() + ".");
    }

    @Override
    @Transactional
    public void transition(AcademicPeriod period, AcademicPeriodStatus expectedStatus, String actionKey,
                          String actorSub, String reference) {
        AcademicPeriodStatus currentStatus = lockPeriodStatus(period.id());
        if (currentStatus != expectedStatus) throw new AcademicPeriodConflictException();
        boolean requireLatestCalendar = "PERIOD_APPROVED".equals(actionKey) || "PERIOD_OPENED".equals(actionKey);
        int changed = jdbcTemplate.update("""
                UPDATE academic_period
                SET status = ?, approved_calendar_revision_id = ?, approval_reference = ?,
                    approved_by = ?, approved_at = ?,
                    opened_by = ?, opened_at = ?, closed_by = ?, closed_at = ?, cancelled_by = ?, cancelled_at = ?
                WHERE period_id = ? AND status = ?
                  AND (? = FALSE OR EXISTS (
                    SELECT 1 FROM academic_calendar_revision selected
                    WHERE selected.calendar_revision_id = ? AND selected.period_id = ?
                      AND selected.status = 'PUBLISHED'
                      AND selected.revision_number = (
                        SELECT MAX(latest.revision_number) FROM academic_calendar_revision latest
                        WHERE latest.period_id = academic_period.period_id AND latest.status = 'PUBLISHED'
                      )
                  ))
                """, period.status().name(), idString(period.approvedCalendarRevisionId()), period.approvalReference(),
                period.approvedBy(), timestamp(period.approvedAt()), period.openedBy(), timestamp(period.openedAt()), period.closedBy(),
                timestamp(period.closedAt()), period.cancelledBy(), timestamp(period.cancelledAt()),
                period.id().toString(), expectedStatus.name(), requireLatestCalendar,
                idString(period.approvedCalendarRevisionId()), period.id().toString());
        if (changed != 1) throw new AcademicPeriodConflictException();
        audit(period.id(), actionKey, actorSub, reference, "Academic period state changed to " + period.status() + ".");
    }

    private AcademicPeriodView mapView(ResultSet rs, int row) throws SQLException {
        int revisionNumber = rs.getInt("calendar_revision_number");
        Integer nullableRevision = rs.wasNull() ? null : revisionNumber;
        return new AcademicPeriodView(mapPeriod(rs), nullableRevision,
                rs.getString("official_reference"), instant(rs, "created_at"));
    }

    private static AcademicPeriod mapPeriod(ResultSet rs) throws SQLException {
        return new AcademicPeriod(
                uuid(rs.getString("period_id")), rs.getString("period_code"),
                AcademicPeriodKind.valueOf(rs.getString("period_kind")), rs.getInt("academic_year"),
                rs.getInt("sequence_number"), rs.getDate("starts_on").toLocalDate(),
                rs.getDate("ends_on").toLocalDate(), AcademicPeriodStatus.valueOf(rs.getString("status")),
                nullableUuid(rs.getString("approved_calendar_revision_id")), rs.getString("approval_reference"),
                rs.getString("approved_by"),
                instant(rs, "approved_at"), rs.getString("opened_by"), instant(rs, "opened_at"),
                rs.getString("closed_by"), instant(rs, "closed_at"), rs.getString("cancelled_by"),
                instant(rs, "cancelled_at"));
    }

    private void audit(UUID periodId, String action, String actor, String reference, String summary) {
        jdbcTemplate.update("""
                INSERT INTO academic_period_audit_event (
                    period_id, action_key, actor_sub, occurred_at, reference, event_summary
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, periodId.toString(), action, actor, nowTimestamp(), reference, summary);
    }

    private Timestamp nowTimestamp() {
        return Timestamp.from(clock.instant());
    }

    private AcademicPeriodStatus lockPeriodStatus(UUID periodId) {
        List<AcademicPeriodStatus> statuses = jdbcTemplate.query("""
                SELECT status FROM academic_period WHERE period_id = ? FOR UPDATE
                """, (rs, row) -> AcademicPeriodStatus.valueOf(rs.getString("status")), periodId.toString());
        if (statuses.isEmpty()) throw new AcademicPeriodNotFoundException();
        return statuses.getFirst();
    }

    private static void requireEditableCalendarState(AcademicPeriodStatus status) {
        if (status != AcademicPeriodStatus.DRAFT && status != AcademicPeriodStatus.APPROVED
                && status != AcademicPeriodStatus.OPEN) {
            throw new AcademicPeriodConflictException();
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC).toInstant();
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), INSTITUTION_ZONE);
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value.trim());
    }

    private static UUID nullableUuid(String value) {
        return value == null ? null : uuid(value);
    }

    private static String idString(UUID id) {
        return id == null ? null : id.toString();
    }

    private record CalendarHeader(UUID id, UUID periodId, int version, String reference,
                                  AcademicCalendarStatus status, String publishedBy, Instant publishedAt) {
    }

    private record CalendarHistoryRow(CalendarHeader header, AcademicCalendarActivity activity) {
    }

    private record CalendarHistoryBuilder(CalendarHeader header, List<AcademicCalendarActivity> activities) {
        private CalendarHistoryBuilder(CalendarHeader header) {
            this(header, new java.util.ArrayList<>());
        }
    }
}
