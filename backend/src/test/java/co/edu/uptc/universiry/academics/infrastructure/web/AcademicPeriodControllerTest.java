package co.edu.uptc.universiry.academics.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-period-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicPeriodControllerTest {

    private static final String READ = "academic:period:read";
    private static final String WRITE = "academic:period:write";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void public_reads_return_only_open_periods_and_admin_reads_require_authentication() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get("/api/v1/academic-periods"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mockMvc.perform(get("/api/v1/admin/academic-periods"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void opening_a_draft_returns_conflict_without_partial_audit_or_state_change() throws Exception {
        // Arrange
        String actor = "period.operator";
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods")
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"2027-DRAFT","kind":"REGULAR","academicYear":2027,
                                 "sequenceNumber":1,"startsOn":"2027-01-15","endsOn":"2027-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID periodId = uuid(created, "id");

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/open", periodId).with(periodWriter(actor)))
                .andExpect(status().isConflict());
        assertEquals("DRAFT", jdbcTemplate.queryForObject(
                "SELECT status FROM academic_period WHERE period_id = ?", String.class, periodId.toString()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_period_audit_event WHERE period_id = ?", Integer.class,
                periodId.toString()));
    }

    @Test
    void cancellation_requires_and_audits_its_official_reference() throws Exception {
        // Arrange
        String actor = "period.operator";
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods")
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"2027-CANCEL","kind":"REGULAR","academicYear":2027,
                                 "sequenceNumber":1,"startsOn":"2027-01-15","endsOn":"2027-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID periodId = uuid(created, "id");

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/cancel", periodId)
                        .with(periodWriter(actor)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/cancel", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"Resolución de cancelación 14 de 2027\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals("Resolución de cancelación 14 de 2027", jdbcTemplate.queryForObject(
                "SELECT reference FROM academic_period_audit_event WHERE period_id = ? AND action_key = 'PERIOD_CANCELLED'",
                String.class, periodId.toString()));
    }

    @Test
    void approval_rejects_an_older_published_calendar_without_transitioning_the_period() throws Exception {
        // Arrange
        String actor = "period.operator";
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods")
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"2027-OLDER-CALENDAR","kind":"REGULAR","academicYear":2027,
                                 "sequenceNumber":1,"startsOn":"2027-01-15","endsOn":"2027-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID periodId = uuid(created, "id");
        UUID olderRevision = uuid(createAndPublishCalendar(periodId, actor,
                "2027-01-10T08:00:00", "2027-01-12T17:00:00"), "id");
        MvcResult latestCalendar = mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officialReference\":\"Resolución 124 de 2027\",\"activities\":["
                                + "{\"key\":\"REGISTRATION\",\"label\":\"Registro final\","
                                + "\"startsAt\":\"2027-01-11T08:00:00\",\"endsAt\":\"2027-01-13T17:00:00\"}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID latestRevision = uuid(latestCalendar, "id");
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars/{revisionId}/publish",
                        periodId, latestRevision).with(periodWriter(actor)))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/approve", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarRevisionId\":\"" + olderRevision
                                + "\",\"approvalReference\":\"Acuerdo aprobatorio 791 de 2027\"}"))
                .andExpect(status().isConflict());
        assertEquals("DRAFT", jdbcTemplate.queryForObject(
                "SELECT status FROM academic_period WHERE period_id = ?", String.class, periodId.toString()));
        assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_period_audit_event WHERE period_id = ?", Integer.class,
                periodId.toString()));
    }

    @Test
    void authorized_operator_can_open_and_close_a_regular_period_with_a_published_calendar() throws Exception {
        // Arrange
        String actor = "period.operator";
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods")
                        .with(periodWriter(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"2027-1","kind":"REGULAR","academicYear":2027,
                                 "sequenceNumber":1,"startsOn":"2027-01-15","endsOn":"2027-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        UUID periodId = uuid(created, "id");
        MvcResult calendar = createAndPublishCalendar(periodId, actor,
                "2027-01-10T08:00:00", "2027-01-12T17:00:00");
        UUID calendarId = uuid(calendar, "id");
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars/{revisionId}/publish",
                        periodId, calendarId).with(periodWriter(actor)))
                .andExpect(status().isConflict());

        // Act
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/approve", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarRevisionId\":\"" + calendarId
                                + "\",\"approvalReference\":\"Acuerdo aprobatorio 789 de 2027\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvalReference").value("Acuerdo aprobatorio 789 de 2027"));
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/open", periodId).with(periodWriter(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        MvcResult amendmentDraft = mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officialReference\":\"Resolución modificatoria 456 de 2027\",\"activities\":["
                                + "{\"key\":\"REGISTRATION\",\"label\":\"Inscripción ajustada\","
                                + "\"startsAt\":\"2027-01-18T08:00:00\",\"endsAt\":\"2027-01-22T17:00:00\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andReturn();
        UUID amendmentId = uuid(amendmentDraft, "id");
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars/{revisionId}/publish",
                        periodId, amendmentId).with(periodWriter(actor)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars/{revisionId}/activate",
                        periodId, amendmentId).with(periodWriter(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.calendarRevisionNumber").value(2))
                .andExpect(jsonPath("$.officialReference").value("Resolución modificatoria 456 de 2027"));
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars/{revisionId}/activate",
                        periodId, calendarId).with(periodWriter(actor)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/academic-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("REGULAR"));
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/close", periodId).with(periodWriter(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(get("/api/v1/admin/academic-periods/{id}/history", periodId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.approvalReference").value("Acuerdo aprobatorio 789 de 2027"))
                .andExpect(jsonPath("$.calendarRevisions.length()").value(2))
                .andExpect(jsonPath("$.calendarRevisions[0].activities[0].startsAt").value("2027-01-10T08:00:00"))
                .andExpect(jsonPath("$.auditEvents.length()").value(9))
                .andExpect(jsonPath("$.auditEvents[3].reference").value("Acuerdo aprobatorio 789 de 2027"));
        mockMvc.perform(get("/api/v1/admin/academic-periods/{id}/history", periodId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officialReference\":\"Resolución posterior\",\"activities\":[]}"))
                .andExpect(status().isConflict());

        // Assert
        assertEquals(9, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_period_audit_event WHERE period_id = ?", Integer.class, periodId.toString()));
        assertEquals("PUBLISHED", jdbcTemplate.queryForObject(
                "SELECT status FROM academic_calendar_revision WHERE calendar_revision_id = ?", String.class,
                calendarId.toString()));
        assertEquals(amendmentId.toString(), jdbcTemplate.queryForObject(
                "SELECT approved_calendar_revision_id FROM academic_period WHERE period_id = ?", String.class,
                periodId.toString()));
    }

    @Test
    void intersemester_period_is_distinct_and_reader_cannot_open_it() throws Exception {
        // Arrange
        String actor = "period.operator";
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods")
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"2027-INT-1","kind":"INTERSEMESTRAL","academicYear":2027,
                                 "sequenceNumber":1,"startsOn":"2027-06-10","endsOn":"2027-07-20"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID periodId = uuid(created, "id");
        MvcResult calendar = createAndPublishCalendar(periodId, actor,
                "2027-07-01T08:00:00", "2027-07-10T17:00:00");
        UUID calendarId = uuid(calendar, "id");
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/approve", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarRevisionId\":\"" + calendarId
                                + "\",\"approvalReference\":\"Acuerdo aprobatorio 790 de 2027\"}"))
                .andExpect(status().isOk());

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/open", periodId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/open", periodId)
                        .with(periodWriter(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("INTERSEMESTRAL"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(get("/api/v1/admin/academic-periods").with(jwt().authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("INTERSEMESTRAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
        mockMvc.perform(get("/api/v1/academic-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("INTERSEMESTRAL"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void calendar_local_times_are_persisted_as_bogota_instants_independent_of_server_timezone() throws Exception {
        // Arrange
        String actor = "period.operator";
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods")
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"2027-TZ","kind":"REGULAR","academicYear":2027,
                                 "sequenceNumber":1,"startsOn":"2027-01-15","endsOn":"2027-06-30"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID periodId = uuid(created, "id");
        MvcResult calendar = createAndPublishCalendar(periodId, actor,
                "2027-01-16T08:00:00", "2027-01-16T17:00:00");
        UUID revisionId = uuid(calendar, "id");

        // Act
        Instant storedStart = jdbcTemplate.queryForObject("""
                SELECT starts_at FROM academic_calendar_activity
                WHERE calendar_revision_id = ? AND activity_key = 'REGISTRATION'
                """, (rs, row) -> rs.getTimestamp("starts_at").toInstant(), revisionId.toString());
        Instant storedEnd = jdbcTemplate.queryForObject("""
                SELECT ends_at FROM academic_calendar_activity
                WHERE calendar_revision_id = ? AND activity_key = 'REGISTRATION'
                """, (rs, row) -> rs.getTimestamp("ends_at").toInstant(), revisionId.toString());

        // Assert
        assertEquals(Instant.parse("2027-01-16T13:00:00Z"), storedStart);
        assertEquals(Instant.parse("2027-01-16T22:00:00Z"), storedEnd);
    }

    private MvcResult createAndPublishCalendar(
            UUID periodId, String actor, String startsAt, String endsAt
    ) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars", periodId)
                        .with(periodWriter(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officialReference\":\"Resolución 123 de 2026\",\"activities\":["
                                + "{\"key\":\"REGISTRATION\",\"label\":\"Inscripción\",\"startsAt\":\""
                                + startsAt + "\",\"endsAt\":\"" + endsAt + "\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        UUID revisionId = uuid(created, "id");
        return mockMvc.perform(post("/api/v1/admin/academic-periods/{id}/calendars/{revisionId}/publish",
                        periodId, revisionId).with(periodWriter(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn();
    }

    private UUID uuid(MvcResult result, String field) throws Exception {
        Matcher match = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        if (!match.find()) throw new IllegalStateException("Missing response field " + field);
        return UUID.fromString(match.group(1));
    }

    private static JwtRequestPostProcessor periodWriter(String subject) {
        return jwt().jwt(token -> token.subject(subject))
                .authorities(new SimpleGrantedAuthority(WRITE));
    }
}
