package co.edu.uptc.universiry.identity.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateRoleAssignmentRequestTest {

    @Test
    void rejects_duplicate_scope_kinds_before_converting_to_a_set() {
        // Arrange
        CreateRoleAssignmentRequest request = new CreateRoleAssignmentRequest(
                UUID.randomUUID(),
                "TEACHER",
                List.of(
                        new CreateRoleAssignmentRequest.ScopeRequest("PROGRAM", UUID.randomUUID().toString()),
                        new CreateRoleAssignmentRequest.ScopeRequest("PROGRAM", UUID.randomUUID().toString())),
                java.time.LocalDate.of(2026, 10, 1),
                null,
                "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, request::toCommand);
    }

    @Test
    void rejects_scope_kind_values_outside_the_closed_enum() {
        // Arrange
        CreateRoleAssignmentRequest request = new CreateRoleAssignmentRequest(
                UUID.randomUUID(),
                "TEACHER",
                List.of(new CreateRoleAssignmentRequest.ScopeRequest("BUILDING", "opaque")),
                java.time.LocalDate.of(2026, 10, 1),
                null,
                "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, request::toCommand);
    }

    @Test
    void rejects_missing_canonical_target_user_id() {
        // Arrange
        CreateRoleAssignmentRequest request = new CreateRoleAssignmentRequest(
                null, "TEACHER", List.of(new CreateRoleAssignmentRequest.ScopeRequest("UNIVERSITY", null)),
                java.time.LocalDate.of(2026, 10, 1), null, "Acta institucional 42");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, request::toCommand);
    }
}
