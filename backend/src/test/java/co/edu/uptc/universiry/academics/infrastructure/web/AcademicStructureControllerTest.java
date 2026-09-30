package co.edu.uptc.universiry.academics.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-structure-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class AcademicStructureControllerTest {

    private static final String READ = "academic:structure:read";
    private static final String WRITE = "academic:structure:write";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void public_structure_returns_empty_normalized_sections_without_authentication() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"units\":[],\"organizationRelations\":[],\"sites\":[],"
                        + "\"siteRelations\":[],\"programAffiliations\":[]}"));
    }

    @Test
    void authorized_operator_can_create_faculty_school_and_site_without_duplicate_labels() throws Exception {
        // Arrange
        UUID facultyId = createUnit("FAC-SCI", "FACULTY", "Facultad de Ciencias", 1);
        UUID schoolId = createUnit("SCH-MATH", "SCHOOL", "Escuela de Matemáticas", 2);
        UUID siteId = createSite("CENTRAL", "CENTRAL", "Sede Central", 1);

        // Act
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}",
                        facultyId, schoolId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo de estructura vigente\"}"))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units.length()").value(2))
                .andExpect(jsonPath("$.organizationRelations.length()").value(1))
                .andExpect(jsonPath("$.sites.length()").value(1))
                .andExpect(jsonPath("$.programAffiliations.length()").value(0))
                .andReturn();

        // Assert
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.indexOf("Facultad de Ciencias")
                < body.indexOf("Escuela de Matemáticas"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains(siteId.toString()) || body.contains("Sede Central"));
        org.junit.jupiter.api.Assertions.assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE actor_sub = ?",
                Integer.class, "structure.operator"));
        org.junit.jupiter.api.Assertions.assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, "Acuerdo"));
    }

    @Test
    void operator_cannot_create_a_cycle_in_the_organization_tree() throws Exception {
        // Arrange
        UUID first = createUnit("UNIT-A", "ACADEMIC_UNIT", "Unidad A", 1);
        UUID second = createUnit("UNIT-B", "ACADEMIC_UNIT", "Unidad B", 2);
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", first, second)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Referencia normativa\"}"))
                .andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", second, first)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Referencia normativa\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void structure_read_permission_does_not_allow_writes_and_anonymous_cannot_write() throws Exception {
        // Arrange
        String body = "{\"code\":\"FAC-READ\",\"type\":\"FACULTY\",\"displayName\":\"Facultad\","
                + "\"displayOrder\":1,\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                + "\"sourceReference\":\"Acuerdo\"}";

        // Act + Assert
        mockMvc.perform(get("/api/v1/admin/academic-structure"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/academic-structure")
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units").isArray());
        mockMvc.perform(post("/api/v1/admin/academic-structure/units").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/academic-structure/units")
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void program_affiliation_reuses_the_program_identity_and_rejects_overlapping_ownership() throws Exception {
        // Arrange
        UUID unitId = createUnit("FAC-ENG", "FACULTY", "Facultad de Ingeniería", 1);
        UUID siteId = createSite("REG-01", "REGIONAL", "Sede Regional", 1);
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_program
                    (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, 'ING-01', 'PREGRADO', 'PRESENCIAL', 'REG-01', CURRENT_TIMESTAMP)
                """, programId.toString());
        String affiliation = "{\"organizationUnitId\":\"" + unitId + "\",\"siteId\":\"" + siteId
                + "\",\"displayOrder\":7,\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                + "\"sourceReference\":\"Acuerdo validado\"}";

        // Act
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(affiliation))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(affiliation.replace("2026-01-01", "2026-06-01")))
                .andExpect(status().isConflict());

        // Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programAffiliations.length()").value(1))
                .andExpect(jsonPath("$.programAffiliations[0].programId").value(programId.toString()))
                .andExpect(jsonPath("$.programAffiliations[0].displayOrder").value(7));
        org.junit.jupiter.api.Assertions.assertEquals(7, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                programId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_id = ?", Integer.class, programId.toString()));
    }

    @Test
    void organization_edges_with_disjoint_effective_dates_do_not_form_a_cycle() throws Exception {
        // Arrange
        UUID first = createUnit("UNIT-HIST-A", "ACADEMIC_UNIT", "Unidad histórica A", 1);
        UUID second = createUnit("UNIT-HIST-B", "ACADEMIC_UNIT", "Unidad histórica B", 2);

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", first, second)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":\"2026-12-31\","
                                + "\"sourceReference\":\"Acto 2026\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", second, first)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2027-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acto 2027\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejects_an_indirect_cycle_in_the_effective_organization_tree() throws Exception {
        // Arrange
        UUID first = createUnit("UNIT-CYCLE-A", "FACULTY", "Unidad ciclo A", 1);
        UUID second = createUnit("UNIT-CYCLE-B", "SCHOOL", "Unidad ciclo B", 2);
        UUID third = createUnit("UNIT-CYCLE-C", "ACADEMIC_UNIT", "Unidad ciclo C", 3);
        createOrganizationEdge(first, second, "2026-01-01", null);
        createOrganizationEdge(second, third, "2026-01-01", null);

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", third, first)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Referencia normativa\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void site_tree_is_ordered_and_rejects_a_cycle() throws Exception {
        // Arrange
        UUID central = createSite("SITE-CENTRAL", "CENTRAL", "Sede Central", 1);
        UUID regional = createSite("SITE-REGION", "REGIONAL", "Sede Regional", 2);
        mockMvc.perform(post("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}", central, regional)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo de sedes\"}"))
                .andExpect(status().isCreated());

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}", regional, central)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo de sedes\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sites[0].code").value("SITE-CENTRAL"))
                .andExpect(jsonPath("$.siteRelations.length()").value(1));
    }

    @Test
    void overlapping_parent_assignments_and_missing_nodes_are_rejected() throws Exception {
        // Arrange
        UUID firstParent = createUnit("PARENT-ONE", "FACULTY", "Facultad Uno", 1);
        UUID secondParent = createUnit("PARENT-TWO", "FACULTY", "Facultad Dos", 2);
        UUID child = createUnit("CHILD-SCHOOL", "SCHOOL", "Escuela", 1);
        createOrganizationEdge(firstParent, child, "2026-01-01", "2026-12-31");

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}",
                        secondParent, child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-06-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}",
                        UUID.randomUUID(), child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2027-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo\"}"))
                .andExpect(status().isNotFound());
    }

    private UUID createUnit(String code, String type, String name, int order) throws Exception {
        String body = "{\"code\":\"" + code + "\",\"type\":\"" + type + "\",\"displayName\":\""
                + name + "\",\"displayOrder\":" + order
                + ",\"validFrom\":\"2026-01-01\",\"validThrough\":null,\"sourceReference\":\"Acuerdo\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/admin/academic-structure/units")
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return responseUuid(result, "id");
    }

    private UUID createSite(String code, String type, String name, int order) throws Exception {
        String body = "{\"code\":\"" + code + "\",\"type\":\"" + type + "\",\"displayName\":\""
                + name + "\",\"displayOrder\":" + order
                + ",\"validFrom\":\"2026-01-01\",\"validThrough\":null,\"sourceReference\":\"Acuerdo\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/admin/academic-structure/sites")
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return responseUuid(result, "id");
    }

    private void createOrganizationEdge(UUID parent, UUID child, String validFrom, String validThrough)
            throws Exception {
        String through = validThrough == null ? "null" : "\"" + validThrough + "\"";
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", parent, child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"" + validFrom + "\",\"validThrough\":" + through
                                + ",\"sourceReference\":\"Referencia normativa\"}"))
                .andExpect(status().isCreated());
    }

    private static UUID responseUuid(MvcResult result, String field) throws Exception {
        Matcher match = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        if (!match.find()) throw new IllegalStateException("Missing response field " + field);
        return UUID.fromString(match.group(1));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor writer() {
        return jwt().jwt(token -> token.subject("structure.operator"))
                .authorities(new SimpleGrantedAuthority(WRITE));
    }
}
