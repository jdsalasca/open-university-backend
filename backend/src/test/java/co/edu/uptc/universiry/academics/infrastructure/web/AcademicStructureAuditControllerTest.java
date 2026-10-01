package co.edu.uptc.universiry.academics.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-structure-audit-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicStructureAuditControllerTest {

    private static final String READ = "academic:structure:read";
    private static final String ENDPOINT = "/api/v1/admin/academic-structure/audit-events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void audit_history_requires_the_academic_structure_read_permission() throws Exception {
        // Arrange
        var anonymousRequest = get(ENDPOINT);
        var authenticatedRequest = get(ENDPOINT).with(jwt().jwt(token -> token.subject("reader")));

        // Act + Assert
        mockMvc.perform(anonymousRequest).andExpect(status().isUnauthorized());
        mockMvc.perform(authenticatedRequest).andExpect(status().isForbidden());
    }

    @Test
    void authorized_reader_gets_a_filtered_newest_first_page_and_can_continue_with_its_cursor() throws Exception {
        // Arrange
        UUID selectedEntity = UUID.randomUUID();
        UUID otherEntity = UUID.randomUUID();
        insertEvent(selectedEntity, "UNIT_CREATED", "unit.created", Instant.parse("2026-09-01T12:00:00Z"));
        insertEvent(otherEntity, "UNIT_CREATED", "other.created", Instant.parse("2026-09-02T12:00:00Z"));
        insertEvent(selectedEntity, "PROGRAM_AFFILIATED", "program.affiliated.one",
                Instant.parse("2026-09-03T12:00:00Z"));
        insertEvent(selectedEntity, "PROGRAM_AFFILIATED", "program.affiliated.two",
                Instant.parse("2026-09-04T12:00:00Z"));
        var authority = new SimpleGrantedAuthority(READ);

        // Act
        var firstPage = mockMvc.perform(get(ENDPOINT)
                        .param("limit", "1")
                        .param("entityId", selectedEntity.toString())
                        .param("actionKey", "PROGRAM_AFFILIATED")
                        .with(jwt().jwt(token -> token.subject("structure.reader")).authorities(authority)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].actionKey").value("PROGRAM_AFFILIATED"))
                .andExpect(jsonPath("$.events[0].entityId").value(selectedEntity.toString()))
                .andExpect(jsonPath("$.events[0].actor").value("structure.operator"))
                .andExpect(jsonPath("$.events[0].reference").value("Acta académica de prueba"))
                .andExpect(jsonPath("$.events[0].summary").value("program.affiliated.two"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn();
        String cursor = com.jayway.jsonpath.JsonPath.read(
                firstPage.getResponse().getContentAsString(), "$.nextCursor");

        var secondPage = mockMvc.perform(get(ENDPOINT)
                        .param("limit", "1")
                        .param("entityId", selectedEntity.toString())
                        .param("actionKey", "PROGRAM_AFFILIATED")
                        .param("before", cursor)
                        .with(jwt().jwt(token -> token.subject("structure.reader")).authorities(authority)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].summary").value("program.affiliated.one"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andReturn();

        // Assert
        org.junit.jupiter.api.Assertions.assertNotEquals(
                firstPage.getResponse().getContentAsString(), secondPage.getResponse().getContentAsString());
    }

    @Test
    void audit_history_returns_empty_page_when_no_matching_events_exist() throws Exception {
        // Arrange
        UUID absentEntity = UUID.randomUUID();

        // Act + Assert
        mockMvc.perform(get(ENDPOINT)
                        .param("entityId", absentEntity.toString())
                        .with(jwt().jwt(token -> token.subject("structure.reader"))
                                .authorities(new SimpleGrantedAuthority(READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void audit_history_rejects_limits_outside_one_to_one_hundred_and_malformed_cursors() throws Exception {
        // Arrange
        var reader = jwt().jwt(token -> token.subject("structure.reader"))
                .authorities(new SimpleGrantedAuthority(READ));

        // Act + Assert
        mockMvc.perform(get(ENDPOINT).param("limit", "101").with(reader))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ENDPOINT).param("limit", "0").with(reader))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ENDPOINT).param("before", "not-a-cursor").with(reader))
                .andExpect(status().isBadRequest());
        String invalidTimestamp = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1|not-an-instant|5".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get(ENDPOINT).param("before", invalidTimestamp).with(reader))
                .andExpect(status().isBadRequest());
        String timestampBeforeMySqlRange = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1|1969-12-31T23:59:59Z|5".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get(ENDPOINT).param("before", timestampBeforeMySqlRange).with(reader))
                .andExpect(status().isBadRequest());
        String timestampAfterMySqlRange = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1|2038-01-19T03:14:07.500000Z|5".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get(ENDPOINT).param("before", timestampAfterMySqlRange).with(reader))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ENDPOINT).param("actionKey", "UNKNOWN_ACTION").with(reader))
                .andExpect(status().isBadRequest());
    }

    private void insertEvent(UUID entityId, String actionKey, String summary, Instant occurredAt) {
        jdbcTemplate.update("""
                INSERT INTO academic_structure_audit_event
                    (entity_id, action_key, actor_sub, source_reference, occurred_at, event_summary)
                VALUES (?, ?, ?, ?, ?, ?)
                """, entityId.toString(), actionKey, "structure.operator", "Acta académica de prueba",
                Timestamp.from(occurredAt), summary);
    }
}
