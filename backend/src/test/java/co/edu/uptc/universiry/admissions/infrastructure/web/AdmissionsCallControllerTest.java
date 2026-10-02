package co.edu.uptc.universiry.admissions.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admissions-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdmissionsCallControllerTest {

    private static final String READ = "admissions:calendar:read";
    private static final String WRITE = "admissions:calendar:write";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void public_endpoint_returns_only_published_calls_and_starts_empty_without_seed_data() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get("/api/v1/admissions/calls"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void administration_requires_a_token_and_separate_calendar_permissions() throws Exception {
        // Arrange + Act + Assert
        mockMvc.perform(get("/api/v1/admin/admissions/calls"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/admissions/calls").with(token("no-permission")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/admissions/calls").with(reader("calendar-reader")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(post("/api/v1/admin/admissions/calls").with(reader("calendar-reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("reader-write-denied")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registered_operator_can_create_edit_and_publish_audited_revision_atomically() throws Exception {
        // Arrange
        String subject = "admissions-operator-" + UUID.randomUUID();
        var actor = writer(subject);
        String me = mockMvc.perform(get("/api/v1/me").with(token(subject)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String canonicalUserId = capture(me, "userId");

        // Act: create an unpublished call, edit its draft, then publish the reviewed version.
        MvcResult created = mockMvc.perform(post("/api/v1/admin/admissions/calls").with(actor)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("pregrado-presencial-2028-i")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latestRevision.status").value("DRAFT"))
                .andExpect(jsonPath("$.latestRevision.revisionNumber").value(1))
                .andReturn();
        String createResponse = created.getResponse().getContentAsString();
        UUID callId = UUID.fromString(capture(createResponse, "id"));
        UUID revisionId = UUID.fromString(captureNth(createResponse, "id", 2));
        mockMvc.perform(get("/api/v1/admissions/calls"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(put("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}", callId, revisionId)
                        .with(actor).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(1, "Contenido revisado por ACRA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftVersion").value(2))
                .andExpect(jsonPath("$.content.milestones[0].description").value("Contenido revisado por ACRA"));

        mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish",
                        callId, revisionId).with(actor).contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody(2, null, "Resolución institucional 12 de 2028")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.officialReference").value("Resolución institucional 12 de 2028"));

        // Assert: the public view includes only the published snapshot and audit links both stable actor IDs.
        mockMvc.perform(get("/api/v1/admissions/calls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].callKey").value("pregrado-presencial-2028-i"))
                .andExpect(jsonPath("$[0].revisionNumber").value(1))
                .andExpect(jsonPath("$[0].content.milestones.length()").value(1))
                .andExpect(jsonPath("$[0].content.milestones[0].description").value("Contenido revisado por ACRA"))
                .andExpect(jsonPath("$[0].officialReference").value("Resolución institucional 12 de 2028"));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admissions_call_audit_event WHERE actor_user_id = ?",
                Integer.class, canonicalUserId));
        assertEquals(3, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM admissions_call_audit_event e
                JOIN institutional_identity i ON i.identity_id = e.actor_identity_id AND i.user_id = e.actor_user_id
                WHERE e.call_id = ?
                """, Integer.class, callId.toString()));
    }

    @Test
    void invalid_url_and_reversed_dates_do_not_create_a_call_or_audit_event() throws Exception {
        // Arrange
        String subject = "invalid-admissions-operator-" + UUID.randomUUID();
        register(subject);

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/admissions/calls").with(writer(subject))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("bad-url", "javascript:alert(1)",
                                "2028-01-01", "2028-01-05", "Fechas publicadas")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/admissions/calls").with(writer(subject))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("bad-dates",
                                "https://acra.example.edu/admissions", "2028-01-05", "2028-01-01", "Fechas publicadas")))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admissions_call", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admissions_call_audit_event", Integer.class));
    }

    @Test
    void stale_edit_and_repeat_publication_return_conflict_without_duplicate_audit() throws Exception {
        // Arrange
        String subject = "stale-admissions-operator-" + UUID.randomUUID();
        register(subject);
        var actor = writer(subject);
        MvcResult created = mockMvc.perform(post("/api/v1/admin/admissions/calls").with(actor)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("pregrado-presencial-stale")))
                .andExpect(status().isCreated()).andReturn();
        String createResponse = created.getResponse().getContentAsString();
        UUID callId = UUID.fromString(capture(createResponse, "id"));
        UUID revisionId = UUID.fromString(captureNth(createResponse, "id", 2));

        // Act + Assert: stale content cannot overwrite an edited or published revision.
        mockMvc.perform(put("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}", callId, revisionId)
                        .with(actor).contentType(MediaType.APPLICATION_JSON).content(updateBody(1, "Primera edición")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}", callId, revisionId)
                        .with(actor).contentType(MediaType.APPLICATION_JSON).content(updateBody(1, "Edición obsoleta")))
                .andExpect(status().isConflict());
        String publication = publishBody(2, null, "Acuerdo 44 de 2028");
        mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish",
                        callId, revisionId).with(actor).contentType(MediaType.APPLICATION_JSON).content(publication))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish",
                        callId, revisionId).with(actor).contentType(MediaType.APPLICATION_JSON).content(publication))
                .andExpect(status().isConflict());

        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admissions_call_audit_event WHERE call_id = ?",
                Integer.class, callId.toString()));
    }

    @Test
    void an_unregistered_external_actor_cannot_write_an_audited_call() throws Exception {
        // Arrange: a valid token and permission without a canonical university-user link.
        String subject = "unregistered-" + UUID.randomUUID();

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/admissions/calls").with(writer(subject))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("not-registered")))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admissions_call", Integer.class));
    }

    @Test
    void a_revision_cannot_publish_when_the_publication_the_operator_reviewed_has_changed() throws Exception {
        // Arrange: publish revision 1, then create revision 2 based on that public state.
        String subject = "revisions-operator-" + UUID.randomUUID();
        register(subject);
        var actor = writer(subject);
        MvcResult created = mockMvc.perform(post("/api/v1/admin/admissions/calls").with(actor)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody("pregrado-presencial-revisions")))
                .andExpect(status().isCreated()).andReturn();
        String response = created.getResponse().getContentAsString();
        UUID callId = UUID.fromString(capture(response, "id"));
        UUID firstRevisionId = UUID.fromString(captureNth(response, "id", 2));
        mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish",
                        callId, firstRevisionId).with(actor).contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody(1, null, "Acuerdo inicial 2028")))
                .andExpect(status().isOk());
        MvcResult second = mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions", callId)
                        .with(actor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":" + contentBody("https://acra.example.edu/admissions",
                                "2028-01-01", "2028-01-05", "Nueva versión") + "}"))
                .andExpect(status().isCreated()).andReturn();
        UUID secondRevisionId = UUID.fromString(capture(second.getResponse().getContentAsString(), "id"));

        // Act + Assert: the stale null expectation fails after another revision became current.
        mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish",
                        callId, secondRevisionId).with(actor).contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody(1, null, "Acuerdo de enmienda 2028")))
                .andExpect(status().isConflict());
        assertEquals(firstRevisionId.toString(), jdbcTemplate.queryForObject(
                "SELECT current_published_revision_id FROM admissions_call WHERE call_id = ?",
                String.class, callId.toString()));
    }

    @Test
    void unknown_call_returns_not_found_without_creating_an_audit_row() throws Exception {
        // Arrange
        String subject = "unknown-call-operator-" + UUID.randomUUID();
        register(subject);

        // Act + Assert
        mockMvc.perform(post("/api/v1/admin/admissions/calls/{callId}/revisions", UUID.randomUUID())
                        .with(writer(subject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":" + contentBody("https://acra.example.edu/admissions",
                                "2028-01-01", "2028-01-05", "Hito") + "}"))
                .andExpect(status().isNotFound());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admissions_call_audit_event", Integer.class));
    }

    private void register(String subject) throws Exception {
        mockMvc.perform(get("/api/v1/me").with(token(subject))).andExpect(status().isOk());
    }

    private JwtRequestPostProcessor token(String subject) {
        return jwt().jwt(jwt -> jwt.issuer("https://identity.example.edu").subject(subject));
    }

    private JwtRequestPostProcessor reader(String subject) {
        return token(subject).authorities(new SimpleGrantedAuthority(READ));
    }

    private JwtRequestPostProcessor writer(String subject) {
        return token(subject).authorities(new SimpleGrantedAuthority(WRITE));
    }

    private String createBody(String callKey) {
        return createBody(callKey, "https://acra.example.edu/admissions", "2028-01-01", "2028-01-05", "Fechas publicadas");
    }

    private String createBody(String callKey, String sourceUrl, String startsOn, String endsOn, String description) {
        return """
                {"callKey":"%s","content":%s}
                """.formatted(callKey, contentBody(sourceUrl, startsOn, endsOn, description));
    }

    private String updateBody(int expectedDraftVersion, String description) {
        return """
                {"expectedDraftVersion":%d,"content":%s}
                """.formatted(expectedDraftVersion,
                contentBody("https://acra.example.edu/admissions", "2028-01-01", "2028-01-05", description));
    }

    private String publishBody(int version, UUID expectedPublishedRevisionId, String reference) {
        return """
                {"expectedDraftVersion":%d,"expectedPublishedRevisionId":%s,"officialReference":"%s"}
                """.formatted(version, expectedPublishedRevisionId == null ? "null" : "\"" + expectedPublishedRevisionId + "\"",
                reference);
    }

    private String contentBody(String sourceUrl, String startsOn, String endsOn, String description) {
        return """
                {"title":"Pregrado presencial 2028-I","callName":"Primer semestre académico de 2028",
                 "updatedAt":"2027-12-15","checkedAt":"2027-12-20",
                 "source":{"label":"Calendario de admisiones","url":"%s"},
                 "confirmationSource":{"label":"Comunicado institucional","url":"https://uptc.example.edu/noticias/admisiones"},
                 "milestones":[{"key":"registration","kind":"APPLICATION","startsOn":"%s",
                   "endsOn":"%s","title":"Inscripción","description":"%s"}]}
                """.formatted(sourceUrl, startsOn, endsOn, description);
    }

    private static String capture(String json, String field) {
        return captureNth(json, field, 1);
    }

    private static String captureNth(String json, String field, int occurrence) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\":\\\"([0-9a-f-]{36})\\\"")
                .matcher(json);
        for (int current = 1; matcher.find(); current++) {
            if (current == occurrence) return matcher.group(1);
        }
        assertTrue(matcher.find(), "expected UUID field " + field + " in JSON");
        throw new AssertionError("expected UUID occurrence " + occurrence + " for field " + field);
    }
}
