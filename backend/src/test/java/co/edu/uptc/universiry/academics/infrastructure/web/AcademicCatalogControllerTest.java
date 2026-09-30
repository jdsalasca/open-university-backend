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
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/import-previews").file(csv))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void anonymous_admin_preview_requires_authentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/import-previews")
                        .file(upload(validCsv(programCode(), "V1", "3"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void preview_rejects_a_missing_jwt_subject_before_validating_the_upload() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/import-previews")
                        .file(upload(validCsv(programCode(), "V1", "INVALID_CREDIT_CELL")))
                        .with(jwt().jwt(token -> token.subject(" ")).authorities(
                                new SimpleGrantedAuthority(READ),
                                new SimpleGrantedAuthority(WRITE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_actor"));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program", Integer.class));
    }

    @Test
    @WithAcademicCatalogPermissions
    void catalog_admin_previews_valid_csv_without_persisting_and_limits_sample_rows() throws Exception {
        // Arrange
        String programCode = programCode();
        String csvContent = curriculumCsv(programCode, "V1", new String[][]{
                {"1", "SUB-ONE", "Álgebra", "3"},
                {"2", "SUB-TWO", "Cálculo", "4"},
                {"2", "SUB-THREE", "Geometría", "2"},
        });

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/import-previews").file(upload(csvContent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programCode").value(programCode))
                .andExpect(jsonPath("$.academicLevel").value("PREGRADO"))
                .andExpect(jsonPath("$.studyModality").value("PRESENCIAL"))
                .andExpect(jsonPath("$.sniesCode").value("12345"))
                .andExpect(jsonPath("$.programName").value("Programa sintético"))
                .andExpect(jsonPath("$.curriculumVersion").value("V1"))
                .andExpect(jsonPath("$.entryCount").value(3))
                .andExpect(jsonPath("$.semesters").isArray())
                .andExpect(jsonPath("$.semesters.length()").value(2))
                .andExpect(jsonPath("$.semesters[0]").value(1))
                .andExpect(jsonPath("$.semesters[1]").value(2))
                .andExpect(jsonPath("$.sampleEntries.length()").value(3))
                .andExpect(jsonPath("$.sampleEntries[0].sourceRowNumber").value(2))
                .andExpect(jsonPath("$.sampleEntries[0].subjectName").value("Álgebra"));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_code = ?", Integer.class, programCode));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_curriculum", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_catalog_audit_event", Integer.class));
    }

    @Test
    @WithAcademicCatalogPermissions
    void invalid_preview_returns_safe_row_issues_and_persists_nothing() throws Exception {
        // Arrange
        String programCode = programCode();
        String secretInvalidCell = "SECRET_INVALID_CREDIT_CELL";

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/import-previews")
                        .file(upload(validCsv(programCode, "V1", secretInvalidCell)))
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_curriculum_csv"))
                .andExpect(jsonPath("$.issues[0].rowNumber").value(2))
                .andExpect(jsonPath("$.issues[0].column").value("credits"))
                .andExpect(content().string(not(containsString(secretInvalidCell))));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_code = ?", Integer.class, programCode));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_catalog_audit_event WHERE actor_sub = ?", Integer.class, ADMIN_USER));
    }

    @Test
    @WithAcademicCatalogPermissions
    void curriculum_preview_reports_all_entries_but_returns_at_most_ten_sample_rows() throws Exception {
        // Arrange
        String programCode = programCode();
        String[][] entries = new String[12][4];
        for (int index = 0; index < entries.length; index++) {
            entries[index] = new String[]{
                    Integer.toString(index + 1),
                    String.format(Locale.ROOT, "SUB-%02d", index + 1),
                    "Asignatura " + (index + 1),
                    "3",
            };
        }

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/import-previews")
                        .file(upload(curriculumCsv(programCode, "V1", entries))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryCount").value(12))
                .andExpect(jsonPath("$.semesters.length()").value(12))
                .andExpect(jsonPath("$.sampleEntries.length()").value(10))
                .andExpect(jsonPath("$.sampleEntries[9].sourceRowNumber").value(11))
                .andExpect(jsonPath("$.sampleEntries[9].subjectCode").value("SUB-10"));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_code = ?", Integer.class, programCode));
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
                .andExpect(jsonPath("$.id").value(curriculumId))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.curriculum").doesNotExist())
                .andExpect(jsonPath("$.entries").doesNotExist());

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumId").value(curriculumId))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].subjectName").value("Asignatura sintética"))
                .andExpect(jsonPath("$.entries[0].semester").value(1));

        // The public capability is GET-only and does not expand to writes on the same route.
        mockMvc.perform(post("/api/v1/academic-catalog/curricula/{id}", curriculumId)
                        .with(catalogAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void published_curriculum_entries_are_returned_in_stable_bounded_pages() throws Exception {
        // Arrange
        String code = programCode();
        String curriculumId = importAndPublish(code, curriculumCsv(code, "V1", new String[][]{
                {"2", subjectCode(), "Gamma", "3"},
                {"1", subjectCode(), "Alfa", "4"},
                {"1", subjectCode(), "Beta", "2"},
        }));

        // Act + Assert: defaults return only the first server-defined page.
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(100))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.entries.length()").value(3));

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("page", "1").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].subjectName").value("Alfa"))
                .andExpect(jsonPath("$.entries[0].semester").value(1))
                .andExpect(jsonPath("$.entries[0].rowOrder").value(2));

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("page", "2").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].subjectName").value("Beta"))
                .andExpect(jsonPath("$.entries[0].rowOrder").value(3));

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("page", "3").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].subjectName").value("Gamma"))
                .andExpect(jsonPath("$.entries[0].semester").value(2))
                .andExpect(jsonPath("$.entries[0].rowOrder").value(1));
    }

    @Test
    void public_entry_search_and_semester_filters_return_filtered_totals() throws Exception {
        // Arrange
        String code = programCode();
        String curriculumId = importAndPublish(code, curriculumCsv(code, "V1", new String[][]{
                {"1", "SUB-SEARCH-01", "Calculo", "3"},
                {"2", "SUB-SEARCH-02", "Fisica", "4"},
                {"1", "SUB-OTHER-03", "Algebra", "2"},
        }));

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("search", "SUB-SEARCH").param("semester", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].subjectCode").value("SUB-SEARCH-01"));
    }

    @Test
    void entry_search_treats_sql_wildcards_literally() throws Exception {
        // Arrange
        String code = programCode();
        String curriculumId = importAndPublish(code, curriculumCsv(code, "V1", new String[][]{
                {"1", subjectCode(), "Asignatura 100%_! literal", "3"},
                {"1", subjectCode(), "Asignatura 100XYZ literal", "4"},
        }));

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("search", "%_!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].subjectName").value("Asignatura 100%_! literal"));
    }

    @Test
    void public_curriculum_entries_hide_missing_and_drafts() throws Exception {
        // Arrange
        String code = programCode();
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports")
                        .file(upload(validCsv(code, "V1", "3")))
                        .with(catalogAdminJwt()))
                .andExpect(status().isCreated());
        String draftId = curriculumIdFor(programIdFor(code));
        String missingId = UUID.randomUUID().toString();

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}", draftId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("curriculum_not_found"));
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", draftId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("curriculum_not_found"));
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("curriculum_not_found"));

        mockMvc.perform(post("/api/v1/academic-catalog/curricula/{id}/entries", draftId)
                        .with(catalogAdminJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejects_invalid_published_entry_page_parameters() throws Exception {
        // Arrange
        String code = programCode();
        String curriculumId = importAndPublish(code, validCsv(code, "V1", "3"));
        String tooLongSearch = "😀".repeat(121);

        // Act + Assert
        assertBadEntryQuery(curriculumId, "page", "0");
        assertBadEntryQuery(curriculumId, "page", "2147483648");
        assertBadEntryQuery(curriculumId, "page", "not-a-number");
        assertBadEntryQuery(curriculumId, "pageSize", "0");
        assertBadEntryQuery(curriculumId, "pageSize", "101");
        assertBadEntryQuery(curriculumId, "semester", "0");
        assertBadEntryQuery(curriculumId, "semester", "32768");
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("search", tooLongSearch)
                        .header("Accept-Language", "es-CO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_curriculum_entries_query"))
                .andExpect(jsonPath("$.message").value("Los parámetros de consulta de asignaturas no son válidos."));

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("page", "2147483647"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2147483647))
                .andExpect(jsonPath("$.entries.length()").value(0));
    }

    @Test
    void returns_zero_pages_and_empty_entries_for_no_matches() throws Exception {
        // Arrange
        String code = programCode();
        String curriculumId = importAndPublish(code, validCsv(code, "V1", "3"));

        // Act + Assert: a filtered empty result has zero pages; an out-of-range page preserves the counts.
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("search", "NO-SUCH-SUBJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.entries.length()").value(0));

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("page", "2").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.entries.length()").value(0));

        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("search", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param("search", "X".repeat(120)).param("semester", "32767"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
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

    private String importAndPublish(String programCode, String csv) throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/academic-catalog/imports")
                        .file(upload(csv))
                        .with(catalogAdminJwt()))
                .andExpect(status().isCreated());
        String curriculumId = curriculumIdFor(programIdFor(programCode));
        mockMvc.perform(post("/api/v1/admin/academic-catalog/curricula/{id}/publish", curriculumId)
                        .with(catalogAdminJwt()))
                .andExpect(status().isOk());
        return curriculumId;
    }

    private void assertBadEntryQuery(String curriculumId, String parameter, String value) throws Exception {
        mockMvc.perform(get("/api/v1/academic-catalog/curricula/{id}/entries", curriculumId)
                        .param(parameter, value)
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_curriculum_entries_query"))
                .andExpect(jsonPath("$.message").value("The curriculum entry query parameters are invalid."));
    }

    private static String curriculumCsv(String programCode, String version, String[][] entries) {
        String headers = "program_code,academic_level,study_modality,snies_code,program_name,faculty,"
                + "campus_code,campus_name,curriculum_version,cohort_from,cohort_through,approval_reference,"
                + "semester,subject_code,subject_name,credits,formation_space,component,choice_group";
        StringBuilder csv = new StringBuilder(headers);
        for (String[] entry : entries) {
            csv.append("\r\n").append(String.join(",", programCode, "PREGRADO", "PRESENCIAL", "12345",
                    "Programa sintético", "Facultad de Ingeniería", "TUNJA", "Tunja", version,
                    "2026-1", "2028-2", "Acuerdo de prueba", entry[0], entry[1], entry[2], entry[3],
                    "Disciplinar", "Obligatorio", ""));
        }
        return csv.append("\r\n").toString();
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
