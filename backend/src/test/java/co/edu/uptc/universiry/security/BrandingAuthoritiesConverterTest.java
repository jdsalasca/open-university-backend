package co.edu.uptc.universiry.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrandingAuthoritiesConverterTest {

    @Test
    void maps_recognized_roles_to_application_permissions() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("institutional_roles", List.of("BRAND_ADMIN", "UNRECOGNIZED", "INSTITUTIONAL_ADMIN"))
                .build();
        BrandingAuthoritiesConverter converter = new BrandingAuthoritiesConverter("institutional_roles");

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertEquals(List.of(
                new SimpleGrantedAuthority("branding:read"),
                new SimpleGrantedAuthority("branding:write")
        ), authorities);
    }

    @Test
    void missing_or_malformed_claim_grants_no_authorities() {
        // Arrange
        Jwt missingClaim = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("other_roles", List.of("BRAND_ADMIN"))
                .build();
        Jwt malformedClaim = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("institutional_roles", "BRAND_ADMIN")
                .build();
        BrandingAuthoritiesConverter converter = new BrandingAuthoritiesConverter("institutional_roles");

        // Act
        var missingAuthorities = converter.convert(missingClaim);
        var malformedAuthorities = converter.convert(malformedClaim);

        // Assert
        assertTrue(missingAuthorities.isEmpty());
        assertTrue(malformedAuthorities.isEmpty());
    }
}
