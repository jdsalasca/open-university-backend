package co.edu.uptc.universiry.identity.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrentIdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymous_request_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void authenticated_user_receives_only_subject_and_known_permissions_without_caching() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("synthetic-subject-42")
                                        .claim("email", "private@example.test"))
                                .authorities(
                                        new SimpleGrantedAuthority("branding:read"),
                                        new SimpleGrantedAuthority("SCOPE_profile")
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("synthetic-subject-42"))
                .andExpect(jsonPath("$.permissions.length()").value(1))
                .andExpect(jsonPath("$.permissions[0]").value("branding:read"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }
}
