package co.edu.uptc.universiry.security;

import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "UPTC_OIDC_AUTHORITIES_CLAIM=institutional_roles",
        "UPTC_OIDC_ROLE_PERMISSION_MAPPING={\"catalog coordinators\":[\"academic:structure:write\"]}"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigurationTest.SecurityErrorMessageConfiguration.class)
class SecurityConfigurationTest {

    private static final String TRANSLATION_WITH_JSON_SPECIAL_CHARACTERS =
            "Texto con \"comillas\", salto de línea\ny fragmento {\"error\":\"falso\"}.";

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void security_bean_uses_the_explicit_server_role_permission_mapping() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("institutional_roles", List.of("catalog coordinators", "BRAND_ADMIN"))
                .build();

        // Act
        var authentication = jwtAuthenticationConverter.convert(jwt);

        // Assert
        assertTrue(authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("academic:structure:write")));
        assertEquals(0, authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().equals("branding:write"))
                .count());
    }

    @Test
    void unauthorized_response_keeps_translated_json_message_as_data() throws Exception {
        // Arrange
        var request = get("/api/v1/me");

        // Act
        var response = mockMvc.perform(request);

        // Assert
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value(TRANSLATION_WITH_JSON_SPECIAL_CHARACTERS));
    }

    @Test
    @WithMockUser(username = "reader")
    void forbidden_response_keeps_translated_json_message_as_data() throws Exception {
        // Arrange
        var request = get("/api/v1/admin/branding");

        // Act
        var response = mockMvc.perform(request);

        // Assert
        response.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value(TRANSLATION_WITH_JSON_SPECIAL_CHARACTERS));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityErrorMessageConfiguration {

        @Bean
        @Primary
        MessageCatalog specialCharacterMessageCatalog() {
            return (key, locale, arguments) -> TRANSLATION_WITH_JSON_SPECIAL_CHARACTERS;
        }
    }
}
