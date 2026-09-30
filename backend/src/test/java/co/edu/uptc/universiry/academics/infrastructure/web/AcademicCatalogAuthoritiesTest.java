package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.security.ApplicationAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicCatalogAuthoritiesTest {

    @Test
    void maps_an_explicit_synthetic_reader_claim_to_read_permission_only() {
        // Arrange
        Jwt jwt = token(List.of("synthetic.catalog.reader"));
        ApplicationAuthoritiesConverter converter = new ApplicationAuthoritiesConverter(
                "institutional_roles", """
                {"synthetic.catalog.reader":["academic:catalog:read"]}
                """);

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertEquals(List.of(new SimpleGrantedAuthority("academic:catalog:read")), authorities);
    }

    @Test
    void maps_an_explicit_synthetic_operator_claim_to_read_and_write_permissions() {
        // Arrange
        Jwt jwt = token(List.of("synthetic.catalog.operator"));
        ApplicationAuthoritiesConverter converter = new ApplicationAuthoritiesConverter(
                "institutional_roles", """
                {"synthetic.catalog.operator":["academic:catalog:read","academic:catalog:write"]}
                """);

        // Act
        var authorities = converter.convert(jwt);

        // Assert
        assertEquals(List.of(
                new SimpleGrantedAuthority("academic:catalog:read"),
                new SimpleGrantedAuthority("academic:catalog:write")
        ), authorities);
    }

    @Test
    void unknown_roles_and_malformed_claims_grant_no_catalog_permissions() {
        // Arrange
        Jwt unknownRole = token(List.of("synthetic.unmapped.role"));
        Jwt malformedClaim = token("synthetic.catalog.operator");
        ApplicationAuthoritiesConverter converter = new ApplicationAuthoritiesConverter(
                "institutional_roles", """
                {"synthetic.catalog.operator":["academic:catalog:read","academic:catalog:write"]}
                """);

        // Act
        var unknownAuthorities = converter.convert(unknownRole);
        var malformedAuthorities = converter.convert(malformedClaim);

        // Assert
        assertTrue(unknownAuthorities.isEmpty());
        assertTrue(malformedAuthorities.isEmpty());
    }

    private static Jwt token(Object roles) {
        return Jwt.withTokenValue("synthetic-test-token")
                .header("alg", "none")
                .claim("institutional_roles", roles)
                .build();
    }
}
