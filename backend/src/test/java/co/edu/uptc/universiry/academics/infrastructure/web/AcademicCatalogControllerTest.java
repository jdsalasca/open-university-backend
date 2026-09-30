package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.CurriculumCsvSchema;
import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import co.edu.uptc.universiry.security.WithAcademicCatalogPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-api-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicCatalogControllerTest {

    private static final String READ = "academic:catalog:read";
    private static final String WRITE = "academic:catalog:write";
    private static final String ADMIN_USER = "catalog.editor";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void anonymous_public_reads_return_empty_catalog_without_authentication() throws Exception {
        // Arrange
        UUID missingProgramId = UUID.randomUUID();

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-catalog/programs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(get("/api/v1/academic-catalog/programs/{id}/curricula", missingProgramId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void anonymous_user_can_download_the_blank_template_from_the_canonical_csv_schema() throws Exception {
        // Arrange
        String expectedTemplate = String.join(",", CurriculumCsvSchema.HEADERS) + "\r\n";

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-catalog/curriculum-template"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("academic-curriculum-template.csv")))
                .andExpect(content().string(expectedTemplate));
    }

    @Test
    @WithMockUser(username = "catalog-reader-test", authorities = READ)
    void curriculum_template_download_is_read_only() throws Exception {
        mockMvc.perform(post("/api/v1/academic-catalog/curriculum-template"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_admin_read_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/academic-catalog/drafts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @WithMockUser(username = "catalog-reader-test", authorities = READ)
    void catalog_reader_can_list_drafts_but_cannot_import() throws Exception {
        // Arrange
        MockMultipartFile csv = upload(validCsv(programCode(), "V1", "3"));

        // Act + Assert
        mockMvc.perform(get("/api/v1/admin/academic-catalog/drafts"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithAcademicCatalogPermissions
    void catalog_admin_imports_a_valid_csv_as_an_audited_draft() throws Exception {
        // Arrange
        String programCode = programCode();
        MockMultipartFile csv = upload(validCsv(programCode, "V1", "3"));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programCode").value(programCode))
                .andExpect(jsonPath("$.curriculumVersion").value("V1"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.entryCount").value(1))
                .andReturn().getResponse().getContentAsString();

        String programId = programIdFor(programCode);
        String curriculumId = curriculumIdFor(programId);
        String sourceHash = jdbcTemplate.queryForObject(
                "SELECT source_sha256 FROM academic_curriculum WHERE curriculum_id = ?",
                String.class, curriculumId);
        org.junit.jupiter.api.Assertions.assertEquals(64, sourceHash.length());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_catalog_audit_event "
                        + "WHERE curriculum_id = ? AND actor_sub = ? AND action_key = 'CURRICULUM_IMPORTED' "
                        + "AND source_sha256 = ?",
                Integer.class, curriculumId, ADMIN_USER, sourceHash));
    }

    @Test
    void missing_jwt_subject_is_rejected_before_parsing_or_persisting_the_upload() throws Exception {
        // Arrange
        String programCode = programCode();
        MockMultipartFile csv = upload(validCsv(programCode, "V1", "INVALID_CREDIT_CELL"));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv)
                        .with(jwt().jwt(token -> token.subject(" ")).authorities(
                                new SimpleGrantedAuthority(READ),
                                new SimpleGrantedAuthority("academic:catalog:write"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_actor"));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_code = ?", Integer.class, programCode));
    }

    @Test
    @WithAcademicCatalogPermissions
    void invalid_csv_returns_safe_row_issues_and_persists_nothing() throws Exception {
        // Arrange
        String programCode = programCode();
        String secretInvalidCell = "SECRET_INVALID_CREDIT_CELL";
        MockMultipartFile csv = upload(validCsv(programCode, "V1", secretInvalidCell));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv)
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_curriculum_csv"))
                .andExpect(jsonPath("$.message").value(
                        "The curriculum file does not match the required format or contains invalid values."))
                .andExpect(jsonPath("$.issues[0].rowNumber").value(2))
                .andExpect(jsonPath("$.issues[0].column").value("credits"))
                .andExpect(content().string(not(containsString(secretInvalidCell))));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_code = ?", Integer.class, programCode));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_catalog_audit_event WHERE actor_sub = ? AND action_key = 'CURRICULUM_IMPORTED'",
                Integer.class, ADMIN_USER));
    }

    @Test
    @WithAcademicCatalogPermissions
    void oversized_csv_returns_413_before_creating_catalog_records() throws Exception {
        // Arrange
        String programCode = programCode();
        MockMultipartFile csv = new MockMultipartFile(
                "file", "oversized.csv", "text/csv", new byte[AcademicCatalogLimits.MAX_IMPORT_BYTES + 1]);

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.error").value("curriculum_too_large"));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_code = ?", Integer.class, programCode));
    }

    @Test
    @WithAcademicCatalogPermissions
    void duplicate_program_version_returns_conflict_without_duplicate_records_or_audits() throws Exception {
        // Arrange
        String programCode = programCode();
        MockMultipartFile csv = upload(validCsv(programCode, "V1", "3"));

        // Act
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv))
                .andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("curriculum_conflict"));

        // Assert
        String programId = jdbcTemplate.queryForObject(
                "SELECT program_id FROM academic_program WHERE program_code = ?", String.class, programCode);
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_curriculum WHERE program_id = ?", Integer.class, programId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_catalog_audit_event e "
                        + "JOIN academic_curriculum c ON c.curriculum_id = e.curriculum_id "
                        + "WHERE c.program_id = ? AND e.action_key = 'CURRICULUM_IMPORTED'",
                Integer.class, programId));
    }

    @Test
    @WithAcademicCatalogPermissions
    void missing_curriculum_details_and_publication_return_not_found() throws Exception {
        // Arrange
        UUID missingId = UUID.randomUUID();

        // Act + Assert
        mockMvc.perform(get("/api/v1/admin/academic-catalog/curricula/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("curriculum_not_found"));
        mockMvc.perform(post("/api/v1/admin/academic-catalog/curricula/{id}/publish", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("curriculum_not_found"));
    }

    @Test
    @WithAcademicCatalogPermissions
    void publication_is_audited_and_public_api_exposes_curriculum_only_after_publish() throws Exception {
        // Arrange
        String programCode = programCode();
        MockMultipartFile csv = upload(validCsv(programCode, "V1", "3"));
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports").file(csv))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String programId = programIdFor(programCode);
        String curriculumId = curriculumIdFor(programId);

        // Act
        mockMvc.perform(get("/api/v1/academic-catalog/programs/{id}/curricula", programId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(post("/api/v1/admin/academic-catalog/curricula/{id}/publish", curriculumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculum.status").value("PUBLISHED"));
        mockMvc.perform(post("/api/v1/admin/academic-catalog/curricula/{id}/publish", curriculumId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("curriculum_conflict"));

        // Assert
        mockMvc.perform(get("/api/v1/academic-catalog/programs/{id}/curricula", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].id").value(curriculumId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_catalog_audit_event "
                        + "WHERE curriculum_id = ? AND action_key = 'CURRICULUM_PUBLISHED' AND actor_sub = ?",
                Integer.class, curriculumId, ADMIN_USER));
    }

    @Test
    void anonymous_curriculum_details_hide_drafts_and_expose_entries_only_after_publication() throws Exception {
        // Arrange
        String code = programCode();
        String csvContent = validCsv(code, "V1", "3");
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports")
                        .file(upload(csvContent))
                        .with(catalogAdminJwt()))
                .andExpect(status().isCreated());
        String curriculumId = curriculumIdFor(programIdFor(code));

        // Act + Assert: public details are indistinguishable from a missing curriculum while still a draft.
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}", curriculumId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("curriculum_not_found"));
        mockMvc.perform(get("/api/v1/admin/academic-catalog/curricula/{id}", curriculumId)
                        .with(catalogAdminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculum.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/admin/academic-catalog/curricula/{id}/publish", curriculumId)
                        .with(catalogAdminJwt()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}", curriculumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculum.id").value(curriculumId))
                .andExpect(jsonPath("$.curriculum.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].subjectName").value("Asignatura sintética"))
                .andExpect(jsonPath("$.entries[0].semester").value(1));

        // The public capability is GET-only and does not expand to writes on the same route.
        mockMvc.perform(post("/api/v1/academic-catalog/curricula/{id}", curriculumId)
                        .with(catalogAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAcademicCatalogPermissions
    void unknown_administration_paths_and_methods_remain_denied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/academic-catalog/not-registered"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
        mockMvc.perform(post("/api/v1/admin/academic-catalog/drafts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    private static MockMultipartFile upload(String csv) {
        return new MockMultipartFile("file", "do-not-persist-this-name.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static RequestPostProcessor catalogAdminJwt() {
        return jwt().jwt(token -> token.subject(ADMIN_USER))
                .authorities(new SimpleGrantedAuthority(READ), new SimpleGrantedAuthority(WRITE));
    }

    private static String validCsv(String programCode, String version, String credits) {
        String headers = "program_code,academic_level,study_modality,snies_code,program_name,faculty,"
                + "campus_code,campus_name,curriculum_version,cohort_from,cohort_through,approval_reference,"
                + "semester,subject_code,subject_name,credits,formation_space,component,choice_group";
        String row = String.join(",", programCode, "PREGRADO", "PRESENCIAL", "12345",
                "Programa sintético", "Facultad de Ingeniería", "TUNJA", "Tunja", version,
                "2026-1", "2028-2", "Acuerdo de prueba", "1", subjectCode(),
                "Asignatura sintética", credits, "Disciplinar", "Obligatorio", "");
        return headers + "\r\n" + row + "\r\n";
    }

    private static String programCode() {
        return "PRG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static String subjectCode() {
        return "SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String programIdFor(String programCode) {
        return jdbcTemplate.queryForObject(
                "SELECT program_id FROM academic_program WHERE program_code = ?", String.class, programCode);
    }

    private String curriculumIdFor(String programId) {
        return jdbcTemplate.queryForObject(
                "SELECT curriculum_id FROM academic_curriculum WHERE program_id = ?", String.class, programId);
    }
}
