package co.edu.uptc.universiry.academics.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    void authorized_operator_can_create_child_unit_and_parent_relation_in_one_audited_operation() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CHILD-CREATE", "FACULTY", "Facultad contenedora", 1,
                "2026-01-01", "2027-12-31");
        String reference = "Acuerdo sintético de estructura";
        String request = "{\"code\":\"SCHOOL-CHILD-CREATE\",\"type\":\"SCHOOL\","
                + "\"displayName\":\"Escuela hija\",\"displayOrder\":7,"
                + "\"validFrom\":\"2026-06-01\",\"validThrough\":\"2027-06-30\","
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children", parentId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        // Assert
        UUID childId = responseUuid(result, "id");
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_unit
                WHERE organization_unit_id = ? AND unit_code = 'SCHOOL-CHILD-CREATE' AND unit_type = 'SCHOOL'
                  AND valid_from = '2026-06-01' AND valid_through = '2027-06-30'
                """, Integer.class, childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ? AND display_order = 7
                  AND valid_from = '2026-06-01' AND valid_through = '2027-06-30'
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND actor_sub = 'structure.operator' AND source_reference = ?
                  AND action_key IN ('UNIT_CREATED', 'UNIT_RELATED')
                """, Integer.class, childId.toString(), reference));
    }

    @Test
    void child_unit_creation_rejects_interval_outside_parent_without_persisting_partial_data() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CHILD-INTERVAL", "FACULTY", "Facultad futura", 1,
                "2027-01-01", null);
        String reference = "Intervalo fuera del padre";
        String request = "{\"code\":\"SCHOOL-CHILD-INTERVAL\",\"type\":\"SCHOOL\","
                + "\"displayName\":\"Escuela fuera de vigencia\",\"displayOrder\":1,"
                + "\"validFrom\":\"2026-06-01\",\"validThrough\":null,"
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children", parentId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_organization_unit WHERE unit_code = 'SCHOOL-CHILD-INTERVAL'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, reference));
    }

    @Test
    void child_unit_creation_requires_an_active_existing_parent() throws Exception {
        // Arrange
        UUID missingParentId = UUID.randomUUID();
        String reference = "Padre inexistente";

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children", missingParentId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SCHOOL-MISSING-PARENT\",\"type\":\"SCHOOL\","
                                + "\"displayName\":\"Escuela sin padre\",\"displayOrder\":1,"
                                + "\"validFrom\":\"2026-06-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"" + reference + "\"}"))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_organization_unit WHERE unit_code = 'SCHOOL-MISSING-PARENT'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, reference));
    }

    @Test
    void child_unit_creation_requires_write_permission() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CHILD-READ-ONLY", "FACULTY", "Facultad de solo lectura", 1);

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/academic-structure/units/{parentId}/children", parentId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SCHOOL-CHILD-READ-ONLY\",\"type\":\"SCHOOL\","
                                + "\"displayName\":\"Escuela solo lectura\",\"displayOrder\":1,"
                                + "\"validFrom\":\"2026-06-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Prueba de solo lectura\"}"))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_organization_unit WHERE unit_code = 'SCHOOL-CHILD-READ-ONLY'",
                Integer.class));
    }

    @Test
    void authorized_operator_can_close_one_dated_hierarchy_relation_without_deleting_its_units() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CLOSE-RELATION", "FACULTY", "Facultad para cerrar", 1,
                "2025-01-01", null);
        UUID childId = createUnit("SCHOOL-CLOSE-RELATION", "SCHOOL", "Escuela para cerrar", 2,
                "2025-01-01", null);
        createOrganizationEdge(parentId, childId, "2025-01-01", "2025-06-30", 1);
        createOrganizationEdge(parentId, childId, "2025-07-01", null, 2);
        String reference = "Acta de reorganización 2026";
        String request = "{\"validFrom\":\"2025-07-01\",\"effectiveThrough\":\"2026-06-30\","
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNoContent());

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ? AND valid_from = '2025-01-01'
                  AND valid_through = '2025-06-30'
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ? AND valid_from = '2025-07-01'
                  AND valid_through = '2026-06-30'
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_unit
                WHERE organization_unit_id IN (?, ?)
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = 'UNIT_RELATION_CLOSED'
                  AND actor_sub = 'structure.operator' AND source_reference = ?
                """, Integer.class, childId.toString(), reference));
    }

    @Test
    void repeated_relation_close_is_idempotent_and_does_not_duplicate_audit() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CLOSE-REPEAT", "FACULTY", "Facultad cierre repetido", 1);
        UUID childId = createUnit("SCHOOL-CLOSE-REPEAT", "SCHOOL", "Escuela cierre repetido", 2);
        createOrganizationEdge(parentId, childId, "2026-01-01", null, 2);
        String request = "{\"validFrom\":\"2026-01-01\",\"effectiveThrough\":\"2026-10-31\","
                + "\"sourceReference\":\"Acta repetida\"}";
        var close = patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/close",
                parentId, childId).with(writer()).contentType(MediaType.APPLICATION_JSON).content(request);

        // Act
        mockMvc.perform(close).andExpect(status().isNoContent());
        mockMvc.perform(close).andExpect(status().isNoContent());

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = 'UNIT_RELATION_CLOSED' AND source_reference = 'Acta repetida'
                """, Integer.class, childId.toString()));
    }

    @Test
    void relation_close_cannot_extend_a_validity_interval_or_create_partial_audit() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CLOSE-EXTEND", "FACULTY", "Facultad cierre extensión", 1);
        UUID childId = createUnit("SCHOOL-CLOSE-EXTEND", "SCHOOL", "Escuela cierre extensión", 2);
        createOrganizationEdge(parentId, childId, "2026-01-01", "2026-06-30", 2);
        String reference = "Referencia de extensión no permitida";
        String request = "{\"validFrom\":\"2026-01-01\",\"effectiveThrough\":\"2026-12-31\","
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ? AND valid_through = '2026-06-30'
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE source_reference = ? AND action_key = 'UNIT_RELATION_CLOSED'
                """, Integer.class, reference));
    }

    @Test
    void relation_close_rejects_an_end_before_the_selected_relation_starts() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CLOSE-RANGE", "FACULTY", "Facultad vigencia", 1);
        UUID childId = createUnit("SCHOOL-CLOSE-RANGE", "SCHOOL", "Escuela vigencia", 2);
        createOrganizationEdge(parentId, childId, "2026-01-01", null, 2);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2026-01-01\",\"effectiveThrough\":\"2025-12-31\","
                                + "\"sourceReference\":\"Referencia de rango inválido\"}"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ? AND valid_through IS NULL
                """, Integer.class, parentId.toString(), childId.toString()));
    }

    @Test
    void relation_close_requires_structure_write_permission() throws Exception {
        // Arrange
        UUID parentId = createUnit("FAC-CLOSE-READ", "FACULTY", "Facultad solo lectura", 1);
        UUID childId = createUnit("SCHOOL-CLOSE-READ", "SCHOOL", "Escuela solo lectura", 2);
        createOrganizationEdge(parentId, childId, "2026-01-01", null, 2);
        String request = "{\"validFrom\":\"2026-01-01\",\"effectiveThrough\":\"2026-10-31\","
                + "\"sourceReference\":\"Referencia de solo lectura\"}";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ? AND valid_through IS NULL
                """, Integer.class, parentId.toString(), childId.toString()));
    }

    @Test
    void authorized_operator_can_close_one_dated_site_relation_without_deleting_its_sites() throws Exception {
        // Arrange
        UUID parentId = createSite("SITE-CLOSE-PARENT", "CENTRAL", "Sede principal cierre", 1,
                "2025-01-01", null);
        UUID childId = createSite("SITE-CLOSE-CHILD", "REGIONAL", "Seccional cierre", 2,
                "2025-01-01", null);
        createSiteEdge(parentId, childId, 1, "2025-01-01", null);
        String reference = "Acta de organización territorial 2026";
        String request = "{\"validFrom\":\"2025-01-01\",\"effectiveThrough\":\"2026-06-30\","
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNoContent());

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_site_relation
                WHERE parent_site_id = ? AND child_site_id = ? AND valid_from = '2025-01-01'
                  AND valid_through = '2026-06-30'
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_site
                WHERE site_id IN (?, ?)
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = 'SITE_RELATION_CLOSED'
                  AND actor_sub = 'structure.operator' AND source_reference = ?
                """, Integer.class, childId.toString(), reference));
    }

    @Test
    void site_relation_close_cannot_extend_a_finite_interval_or_create_partial_audit() throws Exception {
        // Arrange
        UUID parentId = createSite("SITE-CLOSE-EXTEND-PARENT", "CENTRAL", "Sede principal", 1,
                "2025-01-01", null);
        UUID childId = createSite("SITE-CLOSE-EXTEND-CHILD", "REGIONAL", "Sede regional", 2,
                "2025-01-01", null);
        createSiteEdge(parentId, childId, 1, "2025-01-01", "2026-06-30");
        String reference = "Referencia de extensión territorial no permitida";
        String request = "{\"validFrom\":\"2025-01-01\",\"effectiveThrough\":\"2026-12-31\","
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_site_relation
                WHERE parent_site_id = ? AND child_site_id = ? AND valid_through = '2026-06-30'
                """, Integer.class, parentId.toString(), childId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE source_reference = ? AND action_key = 'SITE_RELATION_CLOSED'
                """, Integer.class, reference));
    }

    @Test
    void site_relation_close_requires_structure_write_permission() throws Exception {
        // Arrange
        UUID parentId = createSite("SITE-CLOSE-READ-PARENT", "CENTRAL", "Sede solo lectura", 1,
                "2025-01-01", null);
        UUID childId = createSite("SITE-CLOSE-READ-CHILD", "REGIONAL", "Sede regional solo lectura", 2,
                "2025-01-01", null);
        createSiteEdge(parentId, childId, 1, "2025-01-01", null);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/close",
                        parentId, childId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2025-01-01\",\"effectiveThrough\":\"2026-10-31\","
                                + "\"sourceReference\":\"Referencia de solo lectura\"}"))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_site_relation
                WHERE parent_site_id = ? AND child_site_id = ? AND valid_through IS NULL
                """, Integer.class, parentId.toString(), childId.toString()));
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
                        .with(writer()).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(8, 2)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{siteId}/order", centralSiteId)
                        .with(writer()).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(7, 3)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/order",
                        facultyId, schoolId)
                        .with(writer()).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(6, 1)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/order",
                        centralSiteId, regionalSiteId)
                        .with(writer()).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderChange.formatted(5, 2)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/order",
                        programId, affiliationId)
                        .with(writer()).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
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
    void authorized_operator_can_reassign_program_affiliation_from_effective_date_without_duplicate_program()
            throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-REASSIGN-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-REASSIGN-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-REASSIGN-SOURCE", "CAMPUS", "Lugar origen", 1);
        UUID targetSiteId = createSite("SITE-REASSIGN-TARGET", "REGIONAL", "Lugar destino", 2);
        UUID programId = UUID.randomUUID();
        UUID sourceAffiliationId = createProgramAffiliation(programId, "PROG-REASSIGN-001",
                sourceUnitId, sourceSiteId, "2027-01-01", null);
        String request = """
                {"expectedValidFrom":"2027-01-01","expectedValidThrough":null,
                 "effectiveFrom":"2027-06-01","organizationUnitId":"%s","siteId":"%s",
                 "displayOrder":4,"sourceReference":"Acta de reasignación"}
                """.formatted(targetUnitId, targetSiteId);

        // Act
        MvcResult result = mockMvc.perform(post(
                        "/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/reassign",
                        programId, sourceAffiliationId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        UUID reassignedAffiliationId = responseUuid(result, "id");

        // Assert
        org.junit.jupiter.api.Assertions.assertNotEquals(sourceAffiliationId, reassignedAffiliationId);
        org.junit.jupiter.api.Assertions.assertEquals("2027-05-31", jdbcTemplate.queryForObject(
                "SELECT valid_through FROM academic_program_affiliation WHERE affiliation_id = ?",
                String.class, sourceAffiliationId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_program_affiliation
                WHERE affiliation_id = ? AND program_id = ? AND organization_unit_id = ? AND site_id = ?
                  AND display_order = 4 AND valid_from = '2027-06-01' AND valid_through IS NULL
                  AND source_reference = 'Acta de reasignación'
                """, Integer.class, reassignedAffiliationId.toString(), programId.toString(),
                targetUnitId.toString(), targetSiteId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_id = ?", Integer.class, programId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = 'PROGRAM_AFFILIATION_REASSIGNED'
                  AND actor_sub = 'structure.operator' AND source_reference = ?
                """, Integer.class, reassignedAffiliationId.toString(), "Acta de reasignación"));
    }

    @ParameterizedTest
    @EnumSource(ProgramMove.class)
    void program_affiliation_can_move_only_its_unit_or_only_its_site(ProgramMove move) throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-MOVE-SOURCE-" + move, "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-MOVE-TARGET-" + move, "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-MOVE-SOURCE-" + move, "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-MOVE-TARGET-" + move, "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID sourceAffiliationId = createProgramAffiliation(programId, "PROG-MOVE-" + move,
                sourceUnitId, sourceSiteId, "2027-01-01", null);
        UUID nextUnitId = move == ProgramMove.UNIT_ONLY ? targetUnitId : sourceUnitId;
        UUID nextSiteId = move == ProgramMove.SITE_ONLY ? targetSiteId : sourceSiteId;
        String request = reassignmentRequest("2027-01-01", "null", "2027-06-01", nextUnitId, nextSiteId,
                5, "Acta de movimiento " + move);

        // Act
        MvcResult result = mockMvc.perform(post(reassignmentPath(), programId, sourceAffiliationId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn();
        UUID nextAffiliationId = responseUuid(result, "id");

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_program_affiliation
                WHERE affiliation_id = ? AND program_id = ? AND organization_unit_id = ? AND site_id = ?
                  AND valid_from = '2027-06-01'
                """, Integer.class, nextAffiliationId.toString(), programId.toString(),
                nextUnitId.toString(), nextSiteId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals("2027-05-31", jdbcTemplate.queryForObject(
                "SELECT valid_through FROM academic_program_affiliation WHERE affiliation_id = ?",
                String.class, sourceAffiliationId.toString()));
    }

    @Test
    void stale_expected_dates_and_an_omitted_nullable_end_are_rejected_without_changes() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-STALE-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-STALE-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-STALE-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-STALE-TARGET", "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-STALE", sourceUnitId, sourceSiteId,
                "2027-01-01", null);
        String stale = reassignmentRequest("2027-01-02", "null", "2027-06-01", targetUnitId, targetSiteId,
                4, "Referencia obsoleta");
        String missingEnd = """
                {"expectedValidFrom":"2027-01-01","effectiveFrom":"2027-06-01",
                 "organizationUnitId":"%s","siteId":"%s","displayOrder":4,
                 "sourceReference":"Falta declarar el final esperado"}
                """.formatted(targetUnitId, targetSiteId);

        // Act + Assert
        mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                        .contentType(MediaType.APPLICATION_JSON).content(stale))
                .andExpect(status().isConflict());
        mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                        .contentType(MediaType.APPLICATION_JSON).content(missingEnd))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                programId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE action_key = 'PROGRAM_AFFILIATION_REASSIGNED'
                  AND source_reference IN ('Referencia obsoleta', 'Falta declarar el final esperado')
                """, Integer.class));
    }

    @Test
    void invalid_cutover_dates_and_unchanged_destination_are_conflicts_without_partial_writes() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-DATES-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-DATES-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-DATES-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-DATES-TARGET", "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-DATES", sourceUnitId, sourceSiteId,
                "2027-01-01", "2027-12-31");
        String startsOnSource = reassignmentRequest("2027-01-01", "2027-12-31", "2027-01-01",
                targetUnitId, targetSiteId, 4, "Fecha igual al origen");
        String afterEnd = reassignmentRequest("2027-01-01", "2027-12-31", "2028-01-01",
                targetUnitId, targetSiteId, 4, "Fecha posterior al final");
        String orderOnly = reassignmentRequest("2027-01-01", "2027-12-31", "2027-06-01",
                sourceUnitId, sourceSiteId, 99, "Solo cambia el orden");

        // Act + Assert
        for (String invalidRequest : new String[]{startsOnSource, afterEnd, orderOnly}) {
            mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                            .contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
                    .andExpect(status().isConflict());
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                programId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(java.sql.Date.valueOf("2027-12-31"), jdbcTemplate.queryForObject(
                "SELECT valid_through FROM academic_program_affiliation WHERE affiliation_id = ?",
                java.sql.Date.class, affiliationId.toString()));
    }

    @Test
    void missing_program_affiliation_and_destination_are_not_found() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-MISSING-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-MISSING-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-MISSING-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-MISSING-TARGET", "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-MISSING", sourceUnitId, sourceSiteId,
                "2027-01-01", null);
        String request = reassignmentRequest("2027-01-01", "null", "2027-06-01", targetUnitId, targetSiteId,
                4, "Referencia de destino inexistente");

        // Act + Assert
        mockMvc.perform(post(reassignmentPath(), programId, UUID.randomUUID()).with(writer())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(reassignmentPath(), UUID.randomUUID(), affiliationId).with(writer())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
        String missingTarget = reassignmentRequest("2027-01-01", "null", "2027-06-01", UUID.randomUUID(),
                targetSiteId, 4, "Destino inexistente");
        mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                        .contentType(MediaType.APPLICATION_JSON).content(missingTarget))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                programId.toString()));
    }

    @Test
    void inactive_or_short_lived_destination_cannot_receive_the_program() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-TARGET-RULE-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID sourceSiteId = createSite("SITE-TARGET-RULE-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID inactiveUnitId = createUnit("FAC-TARGET-INACTIVE", "FACULTY", "Facultad inactiva", 2);
        jdbcTemplate.update("UPDATE academic_organization_unit SET status = 'INACTIVE' WHERE organization_unit_id = ?",
                inactiveUnitId.toString());
        UUID activeUnitId = createUnit("FAC-TARGET-SHORT", "FACULTY", "Facultad de vigencia corta", 3,
                "2027-01-01", "2027-11-30");
        UUID shortSiteId = createSite("SITE-TARGET-SHORT", "REGIONAL", "Sede de vigencia corta", 2,
                "2027-07-01", null);
        UUID normalSiteId = createSite("SITE-TARGET-NORMAL", "REGIONAL", "Sede vigente", 3);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-TARGET-RULE", sourceUnitId, sourceSiteId,
                "2027-01-01", "2027-12-31");
        String inactiveTarget = reassignmentRequest("2027-01-01", "2027-12-31", "2027-06-01",
                inactiveUnitId, normalSiteId, 4, "Destino inactivo");
        String insufficientUnit = reassignmentRequest("2027-01-01", "2027-12-31", "2027-06-01",
                activeUnitId, normalSiteId, 4, "Unidad sin vigencia completa");
        String insufficientSite = reassignmentRequest("2027-01-01", "2027-12-31", "2027-06-01",
                activeUnitId, shortSiteId, 4, "Sede sin vigencia completa");

        // Act + Assert
        for (String invalidRequest : new String[]{inactiveTarget, insufficientUnit, insufficientSite}) {
            mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                            .contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
                    .andExpect(status().isConflict());
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                programId.toString()));
    }

    @Test
    void reassignment_detects_overlap_with_an_affiliation_other_than_the_source() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-OVERLAP-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-OVERLAP-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-OVERLAP-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-OVERLAP-TARGET", "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID sourceAffiliationId = createProgramAffiliation(programId, "PROG-OVERLAP", sourceUnitId, sourceSiteId,
                "2027-01-01", "2027-12-31");
        UUID otherAffiliationId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_program_affiliation
                    (affiliation_id, program_id, organization_unit_id, site_id, display_order, valid_from,
                     valid_through, source_reference, created_by, created_at)
                VALUES (?, ?, ?, ?, 8, '2027-10-01', '2027-11-30', 'Legacy overlap fixture',
                        'test.operator', CURRENT_TIMESTAMP)
                """, otherAffiliationId.toString(), programId.toString(), targetUnitId.toString(), targetSiteId.toString());
        String request = reassignmentRequest("2027-01-01", "2027-12-31", "2027-06-01", targetUnitId,
                targetSiteId, 4, "Reasignación con cruce");

        // Act + Assert
        mockMvc.perform(post(reassignmentPath(), programId, sourceAffiliationId).with(writer())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE affiliation_id = ? AND valid_through = ?",
                Integer.class, sourceAffiliationId.toString(), java.sql.Date.valueOf("2027-12-31")));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE affiliation_id = ?",
                Integer.class, otherAffiliationId.toString()));
    }

    @Test
    void reassignment_requires_write_permission_and_valid_request_fields() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-AUTH-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-AUTH-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-AUTH-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-AUTH-TARGET", "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-AUTH", sourceUnitId, sourceSiteId,
                "2027-01-01", null);
        String valid = reassignmentRequest("2027-01-01", "null", "2027-06-01", targetUnitId, targetSiteId,
                4, "Referencia válida");
        String invalidOrder = reassignmentRequest("2027-01-01", "null", "2027-06-01", targetUnitId, targetSiteId,
                -1, "Orden inválido");
        String blankReference = reassignmentRequest("2027-01-01", "null", "2027-06-01", targetUnitId, targetSiteId,
                4, " ");

        // Act + Assert
        mockMvc.perform(post(reassignmentPath(), programId, affiliationId).contentType(MediaType.APPLICATION_JSON)
                        .content(valid))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(reassignmentPath(), programId, affiliationId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isForbidden());
        for (String invalidRequest : new String[]{invalidOrder, blankReference}) {
            mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                            .contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                programId.toString()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failure_to_write_reassignment_audit_rolls_back_both_affiliation_rows() throws Exception {
        // Arrange
        UUID sourceUnitId = createUnit("FAC-AUDIT-ROLLBACK-SOURCE", "FACULTY", "Facultad origen", 1);
        UUID targetUnitId = createUnit("FAC-AUDIT-ROLLBACK-TARGET", "FACULTY", "Facultad destino", 2);
        UUID sourceSiteId = createSite("SITE-AUDIT-ROLLBACK-SOURCE", "CAMPUS", "Sede origen", 1);
        UUID targetSiteId = createSite("SITE-AUDIT-ROLLBACK-TARGET", "REGIONAL", "Sede destino", 2);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-AUDIT-ROLLBACK", sourceUnitId, sourceSiteId,
                "2027-01-01", null);
        String request = reassignmentRequest("2027-01-01", "null", "2027-06-01", targetUnitId, targetSiteId,
                4, "La auditoría debe revertir todo");

        jdbcTemplate.execute("""
                ALTER TABLE academic_structure_audit_event
                ADD CONSTRAINT ck_test_fail_program_reassignment
                CHECK (action_key <> 'PROGRAM_AFFILIATION_REASSIGNED')
                """);
        try {
            // Act + Assert
            mockMvc.perform(post(reassignmentPath(), programId, affiliationId).with(writer())
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isConflict());
            org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM academic_program_affiliation WHERE program_id = ?", Integer.class,
                    programId.toString()));
            org.junit.jupiter.api.Assertions.assertNull(jdbcTemplate.queryForObject(
                    "SELECT valid_through FROM academic_program_affiliation WHERE affiliation_id = ?",
                    java.sql.Date.class, affiliationId.toString()));
            org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM academic_structure_audit_event
                    WHERE action_key = 'PROGRAM_AFFILIATION_REASSIGNED'
                    """, Integer.class));
        } finally {
            jdbcTemplate.execute("ALTER TABLE academic_structure_audit_event "
                    + "DROP CONSTRAINT ck_test_fail_program_reassignment");
            jdbcTemplate.update("DELETE FROM academic_structure_audit_event WHERE entity_id IN (?, ?, ?, ?)",
                    sourceUnitId.toString(), targetUnitId.toString(), sourceSiteId.toString(), targetSiteId.toString());
            jdbcTemplate.update("DELETE FROM academic_program_affiliation WHERE program_id = ?", programId.toString());
            jdbcTemplate.update("DELETE FROM academic_structure_audit_event WHERE source_reference = ?",
                    "La auditoría debe revertir todo");
            jdbcTemplate.update("DELETE FROM academic_program WHERE program_id = ?", programId.toString());
            jdbcTemplate.update("DELETE FROM academic_organization_unit WHERE organization_unit_id IN (?, ?)",
                    sourceUnitId.toString(), targetUnitId.toString());
            jdbcTemplate.update("DELETE FROM academic_site WHERE site_id IN (?, ?)",
                    sourceSiteId.toString(), targetSiteId.toString());
        }
    }

    @Test
    void authorized_operator_can_close_one_dated_program_affiliation_without_deleting_its_records() throws Exception {
        // Arrange
        UUID unitId = createUnit("FAC-CLOSE-AFFILIATION", "FACULTY", "Facultad de cierre", 1);
        UUID siteId = createSite("SITE-CLOSE-AFFILIATION", "REGIONAL", "Sede de cierre", 1);
        UUID programId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO academic_program
                    (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, 'PROG-CLOSE-AFFILIATION', 'PREGRADO', 'PRESENCIAL', 'SITE-CLOSE-AFFILIATION', CURRENT_TIMESTAMP)
                """, programId.toString());
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationUnitId\":\"" + unitId + "\",\"siteId\":\"" + siteId
                                + "\",\"displayOrder\":3,\"validFrom\":\"2027-01-01\",\"validThrough\":null,"
                                + "\"sourceReference\":\"Resolución de adscripción simulada\"}"))
                .andExpect(status().isCreated());
        UUID affiliationId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT affiliation_id FROM academic_program_affiliation WHERE program_id = ?",
                String.class, programId.toString()));
        String reference = "Acta simulada de cierre de adscripción";
        String request = "{\"validFrom\":\"2027-01-01\",\"effectiveThrough\":\"2027-06-30\","
                + "\"sourceReference\":\"" + reference + "\"}";

        // Act
        mockMvc.perform(patch("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/close",
                        programId, affiliationId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/close",
                        programId, affiliationId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNoContent());

        // Assert
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_program_affiliation
                WHERE affiliation_id = ? AND program_id = ? AND organization_unit_id = ? AND site_id = ?
                  AND valid_from = '2027-01-01' AND valid_through = '2027-06-30'
                """, Integer.class, affiliationId.toString(), programId.toString(), unitId.toString(), siteId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_program WHERE program_id = ?", Integer.class, programId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE entity_id = ? AND action_key = 'PROGRAM_AFFILIATION_CLOSED'
                  AND actor_sub = 'structure.operator' AND source_reference = ?
                """, Integer.class, affiliationId.toString(), reference));
        mockMvc.perform(get("/api/v1/admin/academic-structure")
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programAffiliations[0].id").value(affiliationId.toString()))
                .andExpect(jsonPath("$.programAffiliations[0].validThrough").value("2027-06-30"));
    }

    @Test
    void program_affiliation_close_cannot_extend_a_finite_interval_or_create_partial_audit() throws Exception {
        // Arrange
        UUID unitId = createUnit("FAC-CLOSE-AFF-EXT", "FACULTY", "Facultad para extensión", 1);
        UUID siteId = createSite("SITE-CLOSE-AFF-EXT", "REGIONAL", "Sede para extensión", 1);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-CLOSE-AFF-EXT", unitId, siteId,
                "2027-01-01", "2027-06-30");
        String reference = "Referencia de extensión de adscripción";

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/close",
                        programId, affiliationId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2027-01-01\",\"effectiveThrough\":\"2027-12-31\","
                                + "\"sourceReference\":\"" + reference + "\"}"))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_program_affiliation
                WHERE affiliation_id = ? AND valid_through = '2027-06-30'
                """, Integer.class, affiliationId.toString()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_structure_audit_event
                WHERE source_reference = ? AND action_key = 'PROGRAM_AFFILIATION_CLOSED'
                """, Integer.class, reference));
    }

    @Test
    void program_affiliation_close_requires_structure_write_permission() throws Exception {
        // Arrange
        UUID unitId = createUnit("FAC-CLOSE-AFF-READ", "FACULTY", "Facultad solo lectura", 1);
        UUID siteId = createSite("SITE-CLOSE-AFF-READ", "REGIONAL", "Sede solo lectura", 1);
        UUID programId = UUID.randomUUID();
        UUID affiliationId = createProgramAffiliation(programId, "PROG-CLOSE-AFF-READ", unitId, siteId,
                "2027-01-01", null);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/close",
                        programId, affiliationId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(READ)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validFrom\":\"2027-01-01\",\"effectiveThrough\":\"2027-06-30\","
                                + "\"sourceReference\":\"Referencia de solo lectura\"}"))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_program_affiliation
                WHERE affiliation_id = ? AND valid_through IS NULL
                """, Integer.class, affiliationId.toString()));
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

    private UUID createProgramAffiliation(UUID programId, String programCode, UUID unitId, UUID siteId,
                                          String validFrom, String validThrough) throws Exception {
        jdbcTemplate.update("""
                INSERT INTO academic_program
                    (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, ?, 'PREGRADO', 'PRESENCIAL', 'SYNTHETIC-CAMPUS', CURRENT_TIMESTAMP)
                """, programId.toString(), programCode);
        String through = validThrough == null ? "null" : "\"" + validThrough + "\"";
        mockMvc.perform(post("/api/v1/admin/academic-structure/programs/{programId}/affiliations", programId)
                        .with(writer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationUnitId\":\"" + unitId + "\",\"siteId\":\"" + siteId
                                + "\",\"displayOrder\":3,\"validFrom\":\"" + validFrom
                                + "\",\"validThrough\":" + through
                                + ",\"sourceReference\":\"Synthetic affiliation\"}"))
                .andExpect(status().isCreated());
        return UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT affiliation_id FROM academic_program_affiliation WHERE program_id = ?",
                String.class, programId.toString()));
    }

    private static String reassignmentPath() {
        return "/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/reassign";
    }

    private static String reassignmentRequest(String expectedFrom, String expectedThroughJson, String effectiveFrom,
                                              UUID unitId, UUID siteId, int displayOrder, String reference) {
        String expectedThrough = "null".equals(expectedThroughJson)
                ? "null" : "\"" + expectedThroughJson + "\"";
        return """
                {"expectedValidFrom":"%s","expectedValidThrough":%s,"effectiveFrom":"%s",
                 "organizationUnitId":"%s","siteId":"%s","displayOrder":%d,"sourceReference":"%s"}
                """.formatted(expectedFrom, expectedThrough, effectiveFrom, unitId, siteId, displayOrder, reference);
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

    private enum ProgramMove {
        UNIT_ONLY,
        SITE_ONLY
    }
}
