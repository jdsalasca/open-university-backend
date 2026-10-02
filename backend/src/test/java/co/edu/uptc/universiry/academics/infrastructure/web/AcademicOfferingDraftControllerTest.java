package co.edu.uptc.universiry.academics.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-offering-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicOfferingDraftControllerTest {

    private static final String READ = "academic:offerings:read";
    private static final String WRITE = "academic:offerings:write";
    private static final String BASE = "/api/v1/admin/academic-offerings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void administrative_reads_and_writes_require_their_own_permissions() throws Exception {
        // Arrange
        UUID periodId = createPeriod("SECURITY");
        String readerWithoutPermission = "academic.reader";
        String writerWithoutPermission = "academic.writer";

        // Act + Assert
        mockMvc.perform(get(BASE).param("periodId", periodId.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE).param("periodId", periodId.toString())
                        .with(jwt().jwt(token -> token.subject(readerWithoutPermission))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(jwt().jwt(token -> token.subject(writerWithoutPermission))
                                .authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_update_list_and_audit_keep_the_period_separate_and_never_report_available_seats() throws Exception {
        // Arrange
        UUID periodId = createPeriod("INTERSEM");
        CatalogFixture catalog = createPublishedCurriculum();
        String actor = "academic.operator";
        var writer = jwt().jwt(token -> token.subject(actor)).authorities(new SimpleGrantedAuthority(WRITE));
        var reader = jwt().jwt(token -> token.subject("academic.reader")).authorities(new SimpleGrantedAuthority(READ));
        String createBody = draftBody(periodId, catalog, "g-01", 28, "Acta académica 42");

        // Act
        MvcResult created = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody).with(writer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        String offeringId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get(BASE).param("periodId", periodId.toString()).with(reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts.length()").value(1))
                .andExpect(jsonPath("$.drafts[0].sectionCode").value("G-01"))
                .andExpect(jsonPath("$.drafts[0].periodCode").value("INTERSEM-INTERSEM-2027-1"))
                .andExpect(jsonPath("$.drafts[0].periodKind").value("INTERSEMESTRAL"))
                .andExpect(jsonPath("$.drafts[0].programCode").value(catalog.programCode()))
                .andExpect(jsonPath("$.drafts[0].subjectCode").value(catalog.subjectCode()))
                .andExpect(jsonPath("$.drafts[0].proposedCapacity").value(28))
                .andExpect(jsonPath("$.drafts[0].availableSeats").doesNotExist());

        mockMvc.perform(put(BASE + "/{id}", offeringId).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(1, "G-02", 24, "Acta académica 43")).with(writer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(2));

        // Assert
        assertEquals("DRAFT", jdbcTemplate.queryForObject(
                "SELECT status FROM academic_period WHERE period_id = ?", String.class, periodId.toString()));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_offering_draft_audit_event WHERE offering_id = ?",
                Integer.class, offeringId));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT version FROM academic_offering_draft WHERE offering_id = ?", Integer.class, offeringId));
        mockMvc.perform(get(BASE + "/{id}/audit-events", offeringId).with(reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].actionKey").value("OFFERING_DRAFT_UPDATED"))
                .andExpect(jsonPath("$.events[0].before.proposedCapacity").value(28))
                .andExpect(jsonPath("$.events[0].after.proposedCapacity").value(24))
                .andExpect(jsonPath("$.events[0].sourceReference").value("Acta académica 43"))
                .andExpect(jsonPath("$.events[1].actionKey").value("OFFERING_DRAFT_CREATED"));
    }

    @Test
    void duplicate_codes_invalid_capacity_and_unpublished_courses_do_not_leave_partial_rows_or_events() throws Exception {
        // Arrange
        UUID periodId = createPeriod("VALIDATION");
        CatalogFixture published = createPublishedCurriculum();
        CatalogFixture draft = createDraftCurriculum();
        var writer = jwt().jwt(token -> token.subject("academic.operator"))
                .authorities(new SimpleGrantedAuthority(WRITE));
        String valid = draftBody(periodId, published, "G-01", 20, "Acta académica 44");

        // Act
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(valid).with(writer))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(valid).with(writer))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody(periodId, draft, "G-02", 20, "Acta académica 45")).with(writer))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody(periodId, published, "G-03", 0, "Acta académica 46")).with(writer))
                .andExpect(status().isBadRequest());

        // Assert
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM academic_offering_draft", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_offering_draft_audit_event", Integer.class));
    }

    @Test
    void stale_versions_and_bad_page_queries_do_not_add_audit_events() throws Exception {
        // Arrange
        UUID periodId = createPeriod("CONFLICT");
        CatalogFixture catalog = createPublishedCurriculum();
        var writer = jwt().jwt(token -> token.subject("academic.operator"))
                .authorities(new SimpleGrantedAuthority(WRITE));
        var reader = jwt().jwt(token -> token.subject("academic.reader"))
                .authorities(new SimpleGrantedAuthority(READ));
        MvcResult created = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody(periodId, catalog, "G-01", 20, "Acta académica 47")).with(writer))
                .andExpect(status().isCreated()).andReturn();
        String offeringId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Act + Assert
        mockMvc.perform(put(BASE + "/{id}", offeringId).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(1, "G-02", 18, "Acta académica 48")).with(writer))
                .andExpect(status().isOk());
        mockMvc.perform(put(BASE + "/{id}", offeringId).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(1, "G-03", 16, "Acta académica 49")).with(writer))
                .andExpect(status().isConflict());
        mockMvc.perform(get(BASE).param("periodId", periodId.toString()).param("limit", "101").with(reader))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/{id}/audit-events", UUID.randomUUID()).with(reader))
                .andExpect(status().isNotFound());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_offering_draft_audit_event WHERE offering_id = ?",
                Integer.class, offeringId));
    }

    private UUID createPeriod(String suffix) {
        UUID id = UUID.randomUUID();
        LocalDate startsOn = LocalDate.of(2027, 6, 10);
        LocalDate endsOn = LocalDate.of(2027, 7, 30);
        jdbcTemplate.update("""
                INSERT INTO academic_period (period_id, period_code, period_kind, academic_year, sequence_number,
                    starts_on, ends_on, status, created_by, created_at)
                VALUES (?, ?, 'INTERSEMESTRAL', 2027, 1, ?, ?, 'DRAFT', 'synthetic-test-operator', ?)
                """, id.toString(), "INTERSEM-" + suffix + "-2027-1", startsOn, endsOn,
                Timestamp.from(Instant.parse("2026-10-01T12:00:00Z")));
        return id;
    }

    private CatalogFixture createPublishedCurriculum() {
        return createCurriculum("PUBLISHED");
    }

    private CatalogFixture createDraftCurriculum() {
        return createCurriculum("DRAFT");
    }

    private CatalogFixture createCurriculum(String statusValue) {
        UUID programId = UUID.randomUUID();
        UUID programRevisionId = UUID.randomUUID();
        UUID curriculumId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID subjectRevisionId = UUID.randomUUID();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String programCode = "OFF-" + suffix;
        String subjectCode = "SUB-" + suffix;
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 12, 0));
        jdbcTemplate.update("""
                INSERT INTO academic_program (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, ?, 'PREGRADO', 'PRESENCIAL', 'TEST', ?)
                """, programId.toString(), programCode, createdAt);
        jdbcTemplate.update("""
                INSERT INTO academic_program_revision
                    (program_revision_id, program_id, content_fingerprint, snies_code, program_name, faculty, campus_name, created_at)
                VALUES (?, ?, ?, NULL, 'Programa sintético', 'Facultad sintética', 'Sede sintética', ?)
                """, programRevisionId.toString(), programId.toString(), "a".repeat(64), createdAt);
        jdbcTemplate.update("""
                INSERT INTO academic_subject (subject_id, subject_code, created_at) VALUES (?, ?, ?)
                """, subjectId.toString(), subjectCode, createdAt);
        jdbcTemplate.update("""
                INSERT INTO academic_subject_revision
                    (subject_revision_id, subject_id, content_fingerprint, subject_name, credits, created_at)
                VALUES (?, ?, ?, 'Materia sintética', 3.00, ?)
                """, subjectRevisionId.toString(), subjectId.toString(), "b".repeat(64), createdAt);
        jdbcTemplate.update("""
                INSERT INTO academic_curriculum
                    (curriculum_id, program_id, program_revision_id, curriculum_version, cohort_from, cohort_through,
                     approval_reference, status, source_sha256, created_by, created_at, published_by, published_at)
                VALUES (?, ?, ?, 'V1', '2027-1', NULL, 'Aprobación sintética', ?, ?,
                    'synthetic-test-operator', ?, ?, ?)
                """, curriculumId.toString(), programId.toString(), programRevisionId.toString(), statusValue,
                "c".repeat(64), createdAt, statusValue.equals("PUBLISHED") ? "synthetic-test-publisher" : null,
                statusValue.equals("PUBLISHED") ? createdAt : null);
        jdbcTemplate.update("""
                INSERT INTO academic_curriculum_entry
                    (curriculum_id, subject_id, subject_revision_id, semester, formation_space, component,
                     choice_group, row_order, search_subject_code, search_subject_name)
                VALUES (?, ?, ?, 1, 'Formación sintética', 'Obligatoria', NULL, 1, ?, 'Materia sintética')
                """, curriculumId.toString(), subjectId.toString(), subjectRevisionId.toString(), subjectCode);
        return new CatalogFixture(curriculumId, subjectId, programCode, subjectCode);
    }

    private static String draftBody(UUID periodId, CatalogFixture catalog, String sectionCode,
                                    int capacity, String reference) {
        return """
                {"periodId":"%s","curriculumId":"%s","subjectId":"%s","sectionCode":"%s",
                 "startsOn":"2027-06-12","endsOn":"2027-07-20","proposedCapacity":%d,
                 "sourceReference":"%s"}
                """.formatted(periodId, catalog.curriculumId(), catalog.subjectId(), sectionCode, capacity, reference);
    }

    private static String updateBody(int expectedVersion, String sectionCode, int capacity, String reference) {
        return """
                {"expectedVersion":%d,"sectionCode":"%s","startsOn":"2027-06-12",
                 "endsOn":"2027-07-20","proposedCapacity":%d,"sourceReference":"%s"}
                """.formatted(expectedVersion, sectionCode, capacity, reference);
    }

    private record CatalogFixture(UUID curriculumId, UUID subjectId, String programCode, String subjectCode) {
    }
}
