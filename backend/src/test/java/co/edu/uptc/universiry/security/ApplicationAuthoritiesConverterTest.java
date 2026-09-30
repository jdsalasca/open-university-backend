package co.edu.uptc.universiry.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationAuthoritiesConverterTest {

    @Test
    void known_role_names_grant_no_authorities_when_server_mapping_is_not_configured() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("institutional_roles", List.of("BRAND_ADMIN", "ACADEMIC_CATALOG_ADMIN"))
                .build();
        ApplicationAuthoritiesConverter converter = new ApplicationAuthoritiesConverter("institutional_roles");

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertTrue(authorities.isEmpty());
    }

    @Test
    void maps_recognized_roles_to_application_permissions() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("institutional_roles", List.of("branding operators", "catalog readers", "BRAND_ADMIN"))
                .build();
        ApplicationAuthoritiesConverter converter = new ApplicationAuthoritiesConverter("institutional_roles", """
                {"branding operators":["branding:read","branding:write"],
                 "catalog readers":["academic:catalog:read"]}
                """);

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertEquals(List.of(
                new SimpleGrantedAuthority("academic:catalog:read"),
                new SimpleGrantedAuthority("branding:read"),
                new SimpleGrantedAuthority("branding:write")
        ), authorities);
    }

    @Test
    void rejects_unknown_permissions_empty_role_mappings_duplicate_keys_and_malformed_json() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new ApplicationAuthoritiesConverter("roles", """
                {"period operators":["academic:period:admin"]}
                """));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationAuthoritiesConverter("roles", """
                {"period operators":[]}
                """));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationAuthoritiesConverter("roles", """
                {"duplicate":["branding:read"],"duplicate":["branding:write"]}
                """));
        assertThrows(IllegalArgumentException.class,
                () -> new ApplicationAuthoritiesConverter("roles", "not-json"));
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
        ApplicationAuthoritiesConverter converter = new ApplicationAuthoritiesConverter("institutional_roles");

        // Act
        var missingAuthorities = converter.convert(missingClaim);
        var malformedAuthorities = converter.convert(malformedClaim);

        // Assert
        assertTrue(missingAuthorities.isEmpty());
        assertTrue(malformedAuthorities.isEmpty());
    }
}
