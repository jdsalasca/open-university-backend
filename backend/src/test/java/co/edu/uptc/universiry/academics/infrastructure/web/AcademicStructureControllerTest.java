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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                        .content("{\"displayOrder\":4,\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo de estructura vigente\"}"))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units.length()").value(2))
                .andExpect(jsonPath("$.organizationRelations.length()").value(1))
                .andExpect(jsonPath("$.organizationRelations[0].displayOrder").value(4))
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
    void missing_relationship_order_defaults_to_zero_for_existing_clients() throws Exception {
        // Arrange
        UUID parent = createUnit("FAC-DEFAULT-ORDER", "FACULTY", "Facultad orden base", 1);
        UUID child = createUnit("SCHOOL-DEFAULT-ORDER", "SCHOOL", "Escuela orden base", 2);

        // Act
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", parent, child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Acuerdo vigente\"}"))
                .andExpect(status().isCreated());

        // Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationRelations.length()").value(1))
                .andExpect(jsonPath("$.organizationRelations[0].displayOrder").value(0));
    }

    @Test
    void organization_siblings_follow_relationship_order_before_child_node_order() throws Exception {
        // Arrange
        UUID faculty = createUnit("FAC-REL-ORDER", "FACULTY", "Facultad relaciones", 5);
        UUID firstByNode = createUnit("SCHOOL-NODE-FIRST", "SCHOOL", "Escuela nodo primero", 1);
        UUID secondByNode = createUnit("SCHOOL-REL-FIRST", "SCHOOL", "Escuela relación primero", 2);
        UUID tieZulu = createUnit("SCHOOL-TIE-Z", "SCHOOL", "Escuela empate Z", 3);
        UUID tieAlpha = createUnit("SCHOOL-TIE-A", "SCHOOL", "Escuela empate A", 3);
        createOrganizationEdge(faculty, firstByNode, "2026-01-01", null, 8);
        createOrganizationEdge(faculty, secondByNode, "2026-01-01", null, 2);
        createOrganizationEdge(faculty, tieZulu, "2026-01-01", null, 5);
        createOrganizationEdge(faculty, tieAlpha, "2026-01-01", null, 5);

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationRelations.length()").value(4))
                .andExpect(jsonPath("$.organizationRelations[0].childUnitId").value(secondByNode.toString()))
                .andExpect(jsonPath("$.organizationRelations[0].displayOrder").value(2))
                .andExpect(jsonPath("$.organizationRelations[1].childUnitId").value(tieAlpha.toString()))
                .andExpect(jsonPath("$.organizationRelations[1].displayOrder").value(5))
                .andExpect(jsonPath("$.organizationRelations[2].childUnitId").value(tieZulu.toString()))
                .andExpect(jsonPath("$.organizationRelations[2].displayOrder").value(5))
                .andExpect(jsonPath("$.organizationRelations[3].childUnitId").value(firstByNode.toString()))
                .andExpect(jsonPath("$.organizationRelations[3].displayOrder").value(8));
    }

    @Test
    void site_siblings_follow_relationship_order_before_child_node_order() throws Exception {
        // Arrange
        UUID central = createSite("SITE-REL-CENTRAL", "CENTRAL", "Sede Central relaciones", 1);
        UUID firstByNode = createSite("SITE-NODE-FIRST", "REGIONAL", "Sede nodo primero", 1);
        UUID secondByNode = createSite("SITE-REL-FIRST", "REGIONAL", "Sede relación primero", 2);
        UUID tieZulu = createSite("SITE-TIE-Z", "REGIONAL", "Sede empate Z", 3);
        UUID tieAlpha = createSite("SITE-TIE-A", "REGIONAL", "Sede empate A", 3);
        createSiteEdge(central, firstByNode, 8);
        createSiteEdge(central, secondByNode, 2);
        createSiteEdge(central, tieZulu, 5);
        createSiteEdge(central, tieAlpha, 5);

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteRelations.length()").value(4))
                .andExpect(jsonPath("$.siteRelations[0].childSiteId").value(secondByNode.toString()))
                .andExpect(jsonPath("$.siteRelations[0].displayOrder").value(2))
                .andExpect(jsonPath("$.siteRelations[1].childSiteId").value(tieAlpha.toString()))
                .andExpect(jsonPath("$.siteRelations[1].displayOrder").value(5))
                .andExpect(jsonPath("$.siteRelations[2].childSiteId").value(tieZulu.toString()))
                .andExpect(jsonPath("$.siteRelations[2].displayOrder").value(5))
                .andExpect(jsonPath("$.siteRelations[3].childSiteId").value(firstByNode.toString()))
                .andExpect(jsonPath("$.siteRelations[3].displayOrder").value(8));
    }

    @Test
    void public_structure_returns_only_current_relationships_with_their_configured_order() throws Exception {
        // Arrange
        UUID currentParent = createUnit("FAC-CURRENT-REL", "FACULTY", "Facultad vigente", 1);
        UUID currentChild = createUnit("SCHOOL-CURRENT-REL", "SCHOOL", "Escuela vigente", 1);
        createOrganizationEdge(currentParent, currentChild, "2026-01-01", null, 4);
        UUID expiredUnitParent = createUnit("FAC-EXPIRED-REL", "FACULTY", "Facultad vencida", 1,
                "1900-01-01", "1900-12-31");
        UUID expiredUnitChild = createUnit("SCHOOL-EXPIRED-REL", "SCHOOL", "Escuela vencida", 1,
                "1900-01-01", "1900-12-31");
        createOrganizationEdge(expiredUnitParent, expiredUnitChild, "1900-01-01", "1900-12-31", 1);
        UUID futureUnitParent = createUnit("FAC-FUTURE-REL", "FACULTY", "Facultad futura", 1,
                "9999-01-01", null);
        UUID futureUnitChild = createUnit("SCHOOL-FUTURE-REL", "SCHOOL", "Escuela futura", 1,
                "9999-01-01", null);
        createOrganizationEdge(futureUnitParent, futureUnitChild, "9999-01-01", null, 2);

        UUID currentSiteParent = createSite("SITE-CURRENT-REL", "CENTRAL", "Sede vigente", 1);
        UUID currentSiteChild = createSite("SITE-CURRENT-CHILD", "REGIONAL", "Seccional vigente", 1);
        createSiteEdge(currentSiteParent, currentSiteChild, 4);
        UUID expiredSiteParent = createSite("SITE-EXPIRED-REL", "CENTRAL", "Sede vencida", 1,
                "1900-01-01", "1900-12-31");
        UUID expiredSiteChild = createSite("SITE-EXPIRED-CHILD", "REGIONAL", "Seccional vencida", 1,
                "1900-01-01", "1900-12-31");
        createSiteEdge(expiredSiteParent, expiredSiteChild, 1, "1900-01-01", "1900-12-31");
        UUID futureSiteParent = createSite("SITE-FUTURE-REL", "CENTRAL", "Sede futura", 1,
                "9999-01-01", null);
        UUID futureSiteChild = createSite("SITE-FUTURE-CHILD", "REGIONAL", "Seccional futura", 1,
                "9999-01-01", null);
        createSiteEdge(futureSiteParent, futureSiteChild, 2, "9999-01-01", null);

        // Act + Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationRelations.length()").value(1))
                .andExpect(jsonPath("$.organizationRelations[0].childUnitId").value(currentChild.toString()))
                .andExpect(jsonPath("$.organizationRelations[0].displayOrder").value(4))
                .andExpect(jsonPath("$.siteRelations.length()").value(1))
                .andExpect(jsonPath("$.siteRelations[0].childSiteId").value(currentSiteChild.toString()))
                .andExpect(jsonPath("$.siteRelations[0].displayOrder").value(4));
    }

    @Test
    void negative_relationship_order_returns_400_without_persisting_or_auditing() throws Exception {
        // Arrange
        UUID parent = createUnit("FAC-NEGATIVE-ORDER", "FACULTY", "Facultad sin vínculo", 1);
        UUID child = createUnit("SCHOOL-NEGATIVE-ORDER", "SCHOOL", "Escuela sin vínculo", 2);

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", parent, child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayOrder\":-1,\"validFrom\":\"2026-01-01\","
                                + "\"validThrough\":null,\"sourceReference\":\"Orden negativa\"}"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_organization_relation WHERE parent_unit_id = ? AND child_unit_id = ?",
                Integer.class, parent.toString(), child.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, "Orden negativa"));
    }

    @Test
    void authorized_operator_can_correct_all_academic_display_orders_and_audit_each_change() throws Exception {
        // Arrange
        UUID facultyId = createUnit("FAC-ORDER-MAINT", "FACULTY", "Facultad para reordenar", 8);
        UUID schoolId = createUnit("SCHOOL-ORDER-MAINT", "SCHOOL", "Escuela para reordenar", 9);
        UUID centralSiteId = createSite("SITE-ORDER-CENTRAL", "CENTRAL", "Sede central orden", 7);
        UUID regionalSiteId = createSite("SITE-ORDER-REGIONAL", "REGIONAL", "Sede regional orden", 8);
        createOrganizationEdge(facultyId, schoolId, "2026-01-01", null, 6);
        createSiteEdge(centralSiteId, regionalSiteId, 5);
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_program
                    (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, 'ORDER-01', 'PREGRADO', 'PRESENCIAL', 'TUNJA', CURRENT_TIMESTAMP)
                """, programId.toString());
        UUID programUnitId = createUnit("UNIT-PROGRAM-ORDER", "ACADEMIC_UNIT", "Unidad de programa", 1);
        UUID programSiteId = createSite("SITE-PROGRAM-ORDER", "CAMPUS", "Lugar de programa", 1);
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationUnitId\":\"" + programUnitId + "\",\"siteId\":\""
                                + programSiteId + "\",\"displayOrder\":4,\"validFrom\":\"2026-01-01\","
                                + "\"validThrough\":null,\"sourceReference\":\"Adscripción inicial\"}"))
                .andExpect(status().isCreated());
        UUID affiliationId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT affiliation_id FROM academic_program_affiliation WHERE program_id = ?",
                String.class, programId.toString()));

        // Act
        String reference = "Ajuste de orden aprobado";
        String orderChange = "{\"expectedDisplayOrder\":%d,\"displayOrder\":%d,\"sourceReference\":\""
                + reference + "\"}";
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", facultyId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(8, 2)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{siteId}/order", centralSiteId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(7, 3)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/order",
                        facultyId, schoolId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(6, 1)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/order",
                        centralSiteId, regionalSiteId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(5, 2)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/order",
                        programId, affiliationId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(4, 0)))
                .andExpect(status().isNoContent());

        // Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units[?(@.id == '%s')].displayOrder".formatted(facultyId)).value(2))
                .andExpect(jsonPath("$.organizationRelations[?(@.childUnitId == '%s')].displayOrder"
                        .formatted(schoolId)).value(1))
                .andExpect(jsonPath("$.sites[?(@.id == '%s')].displayOrder".formatted(centralSiteId)).value(3))
                .andExpect(jsonPath("$.siteRelations[?(@.childSiteId == '%s')].displayOrder"
                        .formatted(regionalSiteId)).value(2))
                .andExpect(jsonPath("$.programAffiliations[?(@.programId == '%s')].displayOrder"
                        .formatted(programId)).value(0));
        assertOrderAudit(facultyId, "UNIT_ORDER_CHANGED", "Organization unit display order changed from 8 to 2", reference);
        assertOrderAudit(centralSiteId, "SITE_ORDER_CHANGED", "Academic site display order changed from 7 to 3", reference);
        assertOrderAudit(schoolId, "UNIT_RELATION_ORDER_CHANGED",
                "Organization unit relation " + facultyId + "/" + schoolId
                        + " display order changed from 6 to 1", reference);
        assertOrderAudit(regionalSiteId, "SITE_RELATION_ORDER_CHANGED",
                "Academic site relation " + centralSiteId + "/" + regionalSiteId
                        + " display order changed from 5 to 2", reference);
        assertOrderAudit(programId, "PROGRAM_ORDER_CHANGED",
                "Program affiliation " + affiliationId + " display order changed from 4 to 0", reference);
    }

    @Test
    void stale_order_change_conflicts_without_mutating_or_auditing() throws Exception {
        // Arrange
        UUID facultyId = createUnit("FAC-STALE-ORDER", "FACULTY", "Facultad orden desactualizado", 8);
        String request = "{\"expectedDisplayOrder\":3,\"displayOrder\":2,"
                + "\"sourceReference\":\"Ajuste de orden\"}";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", facultyId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(8, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?",
                Integer.class, facultyId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, "Ajuste de orden"));
    }

    @Test
    void missing_inactive_expired_and_future_units_cannot_be_reordered() throws Exception {
        // Arrange
        UUID inactiveUnitId = createUnit("FAC-INACTIVE-ORDER", "FACULTY", "Facultad inactiva", 2);
        UUID expiredUnitId = createUnit("FAC-EXPIRED-ORDER", "FACULTY", "Facultad vencida", 3,
                "1900-01-01", "1900-12-31");
        UUID futureUnitId = createUnit("FAC-FUTURE-ORDER", "FACULTY", "Facultad futura", 4,
                "9999-01-01", null);
        jdbcTemplate.update("UPDATE academic_organization_unit SET status = 'INACTIVE' WHERE organization_unit_id = ?",
                inactiveUnitId.toString());
        String request = "{\"expectedDisplayOrder\":%d,\"displayOrder\":1,"
                + "\"sourceReference\":\"Prioridad fuera de vigencia\"}";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", UUID.randomUUID())
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request.formatted(2)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", inactiveUnitId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request.formatted(2)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", expiredUnitId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request.formatted(3)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", futureUnitId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request.formatted(4)))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?",
                Integer.class, inactiveUnitId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?",
                Integer.class, expiredUnitId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?",
                Integer.class, futureUnitId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, "Prioridad fuera de vigencia"));
    }

    @Test
    void retrying_the_same_order_change_is_idempotent_and_does_not_duplicate_audit() throws Exception {
        // Arrange
        UUID centralSiteId = createSite("SITE-IDEMPOTENT-ORDER", "CENTRAL", "Sede sin cambio", 5);
        String request = "{\"expectedDisplayOrder\":5,\"displayOrder\":2,"
                + "\"sourceReference\":\"Reintento de orden\"}";

        // Act
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{siteId}/order", centralSiteId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{siteId}/order", centralSiteId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNoContent());

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_site WHERE site_id = ?", Integer.class, centralSiteId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, "Reintento de orden"));
    }

    @Test
    void order_change_requires_write_permission_and_valid_values() throws Exception {
        // Arrange
        UUID facultyId = createUnit("FAC-ORDER-PERMISSION", "FACULTY", "Facultad permiso", 4);
        String request = "{\"expectedDisplayOrder\":4,\"displayOrder\":2,"
                + "\"sourceReference\":\"Ajuste de orden\"}";
        String invalidRequest = "{\"expectedDisplayOrder\":4,\"displayOrder\":-1,"
                + "\"sourceReference\":\"Ajuste inválido\"}";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", facultyId)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", facultyId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{unitId}/order", facultyId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT display_order FROM academic_organization_unit WHERE organization_unit_id = ?",
                Integer.class, facultyId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference IN (?, ?)",
                Integer.class, "Ajuste de orden", "Ajuste inválido"));
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
                + "\",\"displayOrder\":7,\"validFrom\":\"2027-01-01\",\"validThrough\":null,"
                + "\"sourceReference\":\"Acuerdo validado\"}";

        // Act
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(affiliation))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content(affiliation.replace("2027-01-01", "2027-06-01")))
                .andExpect(status().isConflict());

        // Assert
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programAffiliations.length()").value(0));
        mockMvc.perform(get("/api/v1/admin/academic-structure")
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programAffiliations.length()").value(1))
                .andExpect(jsonPath("$.programAffiliations[0].programId").value(programId.toString()))
                .andExpect(jsonPath("$.programAffiliations[0].displayOrder").value(7))
                .andExpect(jsonPath("$.programAffiliations[0].validFrom").value("2027-01-01"));
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
        return createUnit(code, type, name, order, "2026-01-01", null);
    }

    private void assertOrderAudit(UUID entityId, String action, String summary, String reference) {
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = ? AND actor_sub = ?
                  AND source_reference = ? AND event_summary = ?
                """, Integer.class, entityId.toString(), action, "structure.operator", reference, summary));
    }

    private UUID createUnit(String code, String type, String name, int order,
                            String validFrom, String validThrough) throws Exception {
        String through = validThrough == null ? "null" : "\"" + validThrough + "\"";
        String body = "{\"code\":\"" + code + "\",\"type\":\"" + type + "\",\"displayName\":\""
                + name + "\",\"displayOrder\":" + order
                + ",\"validFrom\":\"" + validFrom + "\",\"validThrough\":" + through
                + ",\"sourceReference\":\"Acuerdo\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/admin/academic-structure/units")
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return responseUuid(result, "id");
    }

    private UUID createSite(String code, String type, String name, int order) throws Exception {
        return createSite(code, type, name, order, "2026-01-01", null);
    }

    private UUID createSite(String code, String type, String name, int order,
                            String validFrom, String validThrough) throws Exception {
        String through = validThrough == null ? "null" : "\"" + validThrough + "\"";
        String body = "{\"code\":\"" + code + "\",\"type\":\"" + type + "\",\"displayName\":\""
                + name + "\",\"displayOrder\":" + order
                + ",\"validFrom\":\"" + validFrom + "\",\"validThrough\":" + through
                + ",\"sourceReference\":\"Acuerdo\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/admin/academic-structure/sites")
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return responseUuid(result, "id");
    }

    private void createOrganizationEdge(UUID parent, UUID child, String validFrom, String validThrough)
            throws Exception {
        createOrganizationEdge(parent, child, validFrom, validThrough, 0);
    }

    private void createOrganizationEdge(UUID parent, UUID child, String validFrom, String validThrough,
                                        int displayOrder) throws Exception {
        String through = validThrough == null ? "null" : "\"" + validThrough + "\"";
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}", parent, child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayOrder\":" + displayOrder + ",\"validFrom\":\"" + validFrom
                                + "\",\"validThrough\":" + through
                                + ",\"sourceReference\":\"Referencia normativa\"}"))
                .andExpect(status().isCreated());
    }

    private void createSiteEdge(UUID parent, UUID child, int displayOrder) throws Exception {
        createSiteEdge(parent, child, displayOrder, "2026-01-01", null);
    }

    private void createSiteEdge(UUID parent, UUID child, int displayOrder,
                                String validFrom, String validThrough) throws Exception {
        String through = validThrough == null ? "null" : "\"" + validThrough + "\"";
        mockMvc.perform(post("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}", parent, child)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayOrder\":" + displayOrder
                                + ",\"validFrom\":\"" + validFrom + "\",\"validThrough\":" + through + ","
                                + "\"sourceReference\":\"Orden sedes\"}"))
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
