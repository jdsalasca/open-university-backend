package co.edu.uptc.universiry.identity.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Test
    void read_permission_can_list_the_fixed_role_profile_catalog() throws Exception {
        mockMvc.perform(get("/api/v1/admin/access/role-profiles")
                        .with(jwt().jwt(jwt -> jwt
                                .issuer("https://identity.example.edu")
                                .subject("access-manager-1"))
                                .authorities(new SimpleGrantedAuthority("identity:roles:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].key").value("APPLICANT"));
    }

    @Test
    void read_only_authority_cannot_create_a_role_assignment() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/admin/access/assignments")
                        .with(jwt().jwt(jwt -> jwt
                                .issuer("https://identity.example.edu")
                                .subject("access-reader-1"))
                                .authorities(new SimpleGrantedAuthority("identity:roles:read")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_user_cannot_read_identity_search_results() throws Exception {
        mockMvc.perform(get("/api/v1/admin/access/identities")
                        .param("subjectPrefix", "teacher"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administrator_can_register_identities_assign_and_revoke_a_profile_with_audit_versions() throws Exception {
        // Arrange
        String issuer = "https://identity.example.edu";
        String adminSubject = "access-manager-" + UUID.randomUUID();
        String teacherSubject = "teacher-" + UUID.randomUUID();
        String teacherUserId = authenticateOnce(issuer, teacherSubject);
        authenticateOnce(issuer, adminSubject);
        mockMvc.perform(get("/api/v1/admin/access/identities")
                        .param("subjectPrefix", "teacher-")
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(adminSubject))
                                .authorities(new SimpleGrantedAuthority("identity:roles:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(teacherUserId))
                .andExpect(jsonPath("$[0].subject").value(teacherSubject))
                .andExpect(jsonPath("$[0].email").doesNotExist());
        String request = """
                {
                  "targetUserId": "%s",
                  "profileKey": "TEACHER",
                  "scopes": [{"kind": "UNIVERSITY", "reference": null}],
                  "validFrom": "%s",
                  "validThrough": null,
                  "sourceReference": "Acta sintética 2026-42"
                }
                """.formatted(teacherUserId, LocalDate.now(clock).toString());

        // Act
        MvcResult created = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/admin/access/assignments")
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(adminSubject))
                                .authorities(new SimpleGrantedAuthority("identity:roles:write")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileKey").value("TEACHER"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        String assignmentId = objectMapper.readTree(created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("assignmentId").textValue();

        mockMvc.perform(get("/api/v1/me")
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(teacherSubject)
                                .claim("email", "private@example.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(teacherUserId))
                .andExpect(jsonPath("$.assignments[0].profileKey").value("TEACHER"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/admin/access/assignments/{id}/revoke", assignmentId)
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(adminSubject))
                                .authorities(new SimpleGrantedAuthority("identity:roles:write")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": 1, "sourceReference": "Acta sintética 2026-43"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.version").value(2));

        // Assert
        mockMvc.perform(get("/api/v1/admin/access/assignments")
                        .param("userId", teacherUserId)
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(adminSubject))
                                .authorities(new SimpleGrantedAuthority("identity:roles:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetUserId").value(teacherUserId))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].email").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/access/assignments")
                        .param("userId", "not-a-uuid")
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(adminSubject))
                                .authorities(new SimpleGrantedAuthority("identity:roles:read"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/access/assignments")
                        .param("userId", UUID.randomUUID().toString())
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(adminSubject))
                                .authorities(new SimpleGrantedAuthority("identity:roles:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String authenticateOnce(String issuer, String subject) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me")
                        .with(jwt().jwt(jwt -> jwt.issuer(issuer).subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("userId").textValue();
    }
}
