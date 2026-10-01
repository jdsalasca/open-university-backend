package co.edu.uptc.universiry.identity.infrastructure.persistence;

import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.identity.application.RoleAssignmentRepository;
import co.edu.uptc.universiry.identity.domain.AccessAuditAction;
import co.edu.uptc.universiry.identity.domain.AccessAuditEvent;
import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.AssignmentStatus;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.InstitutionalReference;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.identity.domain.RoleProfile;
import co.edu.uptc.universiry.identity.domain.ScopeKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "universiry.mysql-contract.enabled", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityAccessMySqlContractTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Autowired
    private IdentityDirectory identityDirectory;

    @Autowired
    private RoleAssignmentRepository assignmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void mysqlDatasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_URL"));
        properties.add("spring.datasource.username", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Test
    void mysql_84_persists_an_audited_global_role_and_revokes_it_with_one_version_step() {
        // Arrange
        String databaseVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        AuthenticatedPrincipal target = principal("administrator-" + UUID.randomUUID());
        AuthenticatedPrincipal grantor = principal("access-manager-" + UUID.randomUUID());
        identityDirectory.registerIfAbsent(target, NOW);
        identityDirectory.registerIfAbsent(grantor, NOW);
        UUID assignmentId = UUID.randomUUID();
        InstitutionalReference reference = new InstitutionalReference("MySQL identity contract");
        RoleAssignment assignment = new RoleAssignment(
                assignmentId, target, RoleProfile.ADMINISTRATOR,
                Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)),
                TODAY, null, AssignmentStatus.ACTIVE, grantor, reference, NOW, 1);

        // Act
        assignmentRepository.create(assignment, event(assignmentId, AccessAuditAction.GRANTED, grantor, reference, 0, 1));
        RoleAssignment revoked = assignmentRepository.revoke(
                assignmentId, 1, event(assignmentId, AccessAuditAction.REVOKED, grantor, reference, 1, 2));

        // Assert
        assertTrue(databaseVersion != null && databaseVersion.startsWith("8.4."));
        assertEquals(AssignmentStatus.REVOKED, revoked.status());
        assertEquals(2, revoked.version());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_access_audit_event WHERE assignment_id = ?",
                Integer.class, assignmentId.toString()));
        assertTrue(assignmentRepository.findActiveAssignments(target, TODAY).isEmpty());
    }

    @AfterTransaction
    void contract_data_was_rolled_back() {
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM institutional_identity", Integer.class));
    }

    private static AuthenticatedPrincipal principal(String subject) {
        return new AuthenticatedPrincipal("https://identity.example.edu", subject);
    }

    private static AccessAuditEvent event(
            UUID assignmentId,
            AccessAuditAction action,
            AuthenticatedPrincipal actor,
            InstitutionalReference reference,
            long previousVersion,
            long version) {
        return new AccessAuditEvent(UUID.randomUUID(), assignmentId, action, actor,
                NOW, reference, previousVersion, version);
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required MySQL contract environment variable is missing: " + key);
        }
        return value;
    }
}
