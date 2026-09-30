package co.edu.uptc.universiry.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "UPTC_OIDC_AUTHORITIES_CLAIM=institutional_roles",
        "UPTC_OIDC_ROLE_PERMISSION_MAPPING={\"catalog coordinators\":[\"academic:structure:write\"]}"
})
@ActiveProfiles("test")
class SecurityConfigurationTest {

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

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
}
