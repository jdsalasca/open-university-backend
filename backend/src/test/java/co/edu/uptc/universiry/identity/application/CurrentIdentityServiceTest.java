package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.security.ApplicationPermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentIdentityServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Mock
    private IdentityDirectory identities;

    @Mock
    private RoleAssignmentRepository assignments;

    @Test
    void registers_only_the_issuer_subject_pair_and_returns_known_permissions() {
        // Arrange
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                "https://identity.example.edu", "synthetic-subject-1");
        UUID userId = UUID.randomUUID();
        RegisteredIdentity registered = new RegisteredIdentity(UUID.randomUUID(), userId, principal, NOW);
        when(identities.registerIfAbsent(principal, NOW)).thenReturn(registered);
        when(assignments.findActiveAssignments(userId, NOW.atZone(ZoneId.of("America/Bogota")).toLocalDate()))
                .thenReturn(List.of());
        CurrentIdentityService service = new CurrentIdentityService(identities, assignments,
                Clock.fixed(NOW, ZoneId.of("America/Bogota")));

        // Act
        CurrentIdentitySnapshot result = service.currentIdentity(principal,
                List.of(ApplicationPermission.BRANDING_READ));

        // Assert
        verify(identities).registerIfAbsent(principal, NOW);
        verify(assignments).findActiveAssignments(userId, NOW.atZone(ZoneId.of("America/Bogota")).toLocalDate());
        assertEquals(userId, result.userId());
        assertEquals("synthetic-subject-1", result.subject());
        assertEquals(List.of("branding:read"), result.permissions());
        assertEquals(List.of(), result.assignments());
    }
}
