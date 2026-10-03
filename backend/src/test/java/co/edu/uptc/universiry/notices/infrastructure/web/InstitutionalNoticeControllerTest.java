package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.security.WithNoticePermissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:institutional-notice-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstitutionalNoticeControllerTest {

    private static final String NOTICES = "/api/v1/admin/notices";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reading_notices_requires_an_institutional_session() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(NOTICES))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "visitor")
    void reading_notices_requires_the_notice_read_permission() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(NOTICES))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "visitor", authorities = "notices:read")
    void publishing_requires_the_notice_write_permission() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(post(NOTICES).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithNoticePermissions
    void publishing_records_the_notice_its_audiences_and_its_audit_event() throws Exception {
        // Arrange + Act
        var created = mockMvc.perform(post(NOTICES).contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noticeId").isString())
                .andExpect(jsonPath("$.audiences", hasSize(2)))
                .andReturn();

        // Assert
        String noticeId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.noticeId");
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM institutional_notice WHERE notice_id = ?", Integer.class, noticeId));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM institutional_notice_audience WHERE notice_id = ?", Integer.class, noticeId));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM institutional_notice_audit_event
                WHERE notice_id = ? AND action_key = 'NOTICE_PUBLISHED' AND actor_sub = 'notices.editor'
                """, Integer.class, noticeId));
        mockMvc.perform(get(NOTICES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices", hasSize(1)))
                .andExpect(jsonPath("$.notices[0].sourceReference").value("Resolución 1 de 2026"));
    }

    @Test
    @WithNoticePermissions
    void publishing_rejects_an_audience_scope_that_the_domain_does_not_allow() throws Exception {
        // Arrange
        String body = """
                {"title":"Aviso","body":"Cuerpo","sourceReference":"Resolución 1 de 2026",
                 "publishedFrom":"2026-10-03","publishedThrough":"2026-10-10",
                 "audiences":[{"kind":"UNIVERSITY","reference":"TUNJA"}]}""";

        // Act + Assert
        mockMvc.perform(post(NOTICES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM institutional_notice", Integer.class));
    }

    @Test
    @WithNoticePermissions
    void publishing_rejects_an_inverted_availability_window() throws Exception {
        // Arrange
        String body = """
                {"title":"Aviso","body":"Cuerpo","sourceReference":"Resolución 1 de 2026",
                 "publishedFrom":"2026-10-10","publishedThrough":"2026-10-03",
                 "audiences":[{"kind":"UNIVERSITY"}]}""";

        // Act + Assert
        mockMvc.perform(post(NOTICES).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithNoticePermissions
    void reading_notices_rejects_a_page_larger_than_the_contract_limit() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get(NOTICES).param("limit", "101"))
                .andExpect(status().isBadRequest());
    }

    private static void assertEquals(int expected, Integer actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static String validBody() {
        return """
                {"title":"Cierre temporal de una sede","body":"Cuerpo del aviso institucional.",
                 "sourceReference":"Resolución 1 de 2026",
                 "publishedFrom":"2026-10-03","publishedThrough":"2026-10-10",
                 "audiences":[{"kind":"SITE","reference":"TUNJA"},{"kind":"PROGRAM","reference":"ING-SIS"}]}""";
    }
}