package co.edu.uptc.universiry.security;

import co.edu.uptc.universiry.identity.application.RoleAssignmentRepository;
import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.AssignmentStatus;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.InstitutionalReference;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.identity.domain.RoleProfile;
import co.edu.uptc.universiry.identity.domain.ScopeKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveAuthoritiesConverterTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final LocalDate TODAY = NOW.atZone(ZoneId.of("America/Bogota")).toLocalDate();

    @Mock
    private RoleAssignmentRepository assignments;

    @Test
    void combines_only_explicit_oidc_permissions_with_active_role_permissions() {
        // Arrange
        Jwt token = Jwt.withTokenValue("synthetic-token")
                .header("alg", "none")
                .issuer("https://identity.example.edu")
                .subject("admin-1")
                .claim("authorities", List.of("UPTC_PORTAL_ADMIN"))
                .build();
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                "https://identity.example.edu", "admin-1");
        RoleAssignment admin = new RoleAssignment(
                UUID.randomUUID(), principal, RoleProfile.ADMINISTRATOR,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, AssignmentStatus.ACTIVE,
                new AuthenticatedPrincipal("https://identity.example.edu", "bootstrap-1"),
                new InstitutionalReference("Acta institucional 42"), NOW, 1);
        when(assignments.findActiveAssignments(principal, TODAY)).thenReturn(List.of(admin));
        ApplicationAuthoritiesConverter configuredClaims = new ApplicationAuthoritiesConverter(
                "authorities", "{\"UPTC_PORTAL_ADMIN\":[\"branding:read\"]}");
        EffectiveAuthoritiesConverter converter = new EffectiveAuthoritiesConverter(
                configuredClaims, assignments, Clock.fixed(NOW, ZoneId.of("America/Bogota")));

        // Act
        List<String> authorities = converter.convert(token).stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        // Assert
        verify(assignments).findActiveAssignments(principal, TODAY);
        assertEquals(List.of("branding:read", "identity:roles:read", "identity:roles:write"), authorities);
    }
}
