package co.edu.uptc.universiry.identity.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalUserMigrationIntegrationTest {

    private static final Instant FIRST_SEEN = Instant.parse("2026-10-01T12:00:00Z");
    private SingleConnectionDataSource activeDataSource;

    @org.junit.jupiter.api.AfterEach
    void closeDatabase() {
        if (activeDataSource != null) {
            activeDataSource.destroy();
        }
    }

    @Test
    void v21_preserves_legacy_identity_role_scope_and_audit_links_while_backfilling_users() throws Exception {
        // Arrange
        SingleConnectionDataSource dataSource = dataSource();
        activeDataSource = dataSource;
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("20"))
                .load()
                .migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        UUID targetIdentityId = UUID.randomUUID();
        UUID grantorIdentityId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID auditEventId = UUID.randomUUID();
        insertIdentity(jdbc, targetIdentityId, "target-" + targetIdentityId);
        insertIdentity(jdbc, grantorIdentityId, "grantor-" + grantorIdentityId);
        jdbc.update("""
                INSERT INTO identity_role_assignment (
                    assignment_id, target_identity_id, profile_key, status, valid_from, valid_through,
                    source_reference, granted_by_identity_id, created_at, version
                ) VALUES (?, ?, 'TEACHER', 'ACTIVE', DATE '2026-10-01', NULL, ?, ?, ?, 1)
                """, assignmentId.toString(), targetIdentityId.toString(), "Acta sintética 2026-42",
                grantorIdentityId.toString(), Timestamp.from(FIRST_SEEN));
        jdbc.update("""
                INSERT INTO identity_role_assignment_scope (
                    assignment_id, scope_kind, site_id, organization_unit_id, program_id,
                    job_appointment_reference
                ) VALUES (?, 'UNIVERSITY', NULL, NULL, NULL, NULL)
                """, assignmentId.toString());
        jdbc.update("""
                INSERT INTO identity_access_audit_event (
                    audit_event_id, assignment_id, action_key, actor_identity_id, occurred_at,
                    source_reference, previous_version, version
                ) VALUES (?, ?, 'GRANTED', ?, ?, ?, 0, 1)
                """, auditEventId.toString(), assignmentId.toString(), grantorIdentityId.toString(),
                Timestamp.from(FIRST_SEEN), "Acta sintética 2026-42");

        // Act
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        // Assert
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM university_user", Integer.class));
        assertEquals(assignmentId.toString(), jdbc.queryForObject(
                "SELECT assignment_id FROM identity_role_assignment WHERE assignment_id = ?", String.class,
                assignmentId.toString()));
        assertEquals(auditEventId.toString(), jdbc.queryForObject(
                "SELECT audit_event_id FROM identity_access_audit_event WHERE audit_event_id = ?", String.class,
                auditEventId.toString()));
        assertEquals(targetIdentityId.toString(), jdbc.queryForObject(
                "SELECT user_id FROM institutional_identity WHERE identity_id = ?", String.class,
                targetIdentityId.toString()));
        assertEquals(targetIdentityId.toString(), jdbc.queryForObject(
                "SELECT target_user_id FROM identity_role_assignment WHERE assignment_id = ?", String.class,
                assignmentId.toString()));
        assertEquals(grantorIdentityId.toString(), jdbc.queryForObject(
                "SELECT granted_by_user_id FROM identity_role_assignment WHERE assignment_id = ?", String.class,
                assignmentId.toString()));
        assertEquals(grantorIdentityId.toString(), jdbc.queryForObject(
                "SELECT actor_user_id FROM identity_access_audit_event WHERE audit_event_id = ?", String.class,
                auditEventId.toString()));
        assertEquals(grantorIdentityId.toString(), jdbc.queryForObject(
                "SELECT actor_identity_id FROM identity_access_audit_event WHERE audit_event_id = ?", String.class,
                auditEventId.toString()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM identity_role_assignment_scope WHERE assignment_id = ?", Integer.class,
                assignmentId.toString()));
        assertEquals("Acta sintética 2026-42", jdbc.queryForObject(
                "SELECT source_reference FROM identity_access_audit_event WHERE audit_event_id = ?", String.class,
                auditEventId.toString()));
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'identity_role_assignment' AND column_name = 'target_identity_id'
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'identity_role_assignment' AND column_name = 'granted_by_identity_id'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'identity_access_audit_event' AND column_name = 'actor_identity_id'
                """, Integer.class));
    }

    @Test
    void v21_creates_an_empty_canonical_user_table_without_seeding_users() throws Exception {
        // Arrange
        SingleConnectionDataSource dataSource = dataSource();
        activeDataSource = dataSource;
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Act / Assert
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM university_user", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM institutional_identity", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM identity_role_assignment", Integer.class));
    }

    private static SingleConnectionDataSource dataSource() {
        String url = "jdbc:h2:mem:canonical_user_migration_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        return new SingleConnectionDataSource(url, "sa", "", true);
    }

    private static void insertIdentity(JdbcTemplate jdbc, UUID identityId, String subject) {
        String issuer = "https://identity.example.edu";
        byte[] issuerDigest;
        try {
            issuerDigest = MessageDigest.getInstance("SHA-256").digest(issuer.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        jdbc.update("""
                INSERT INTO institutional_identity (
                    identity_id, issuer, subject, issuer_sha256, first_seen_at
                ) VALUES (?, ?, ?, ?, ?)
                """, identityId.toString(), issuer,
                subject.getBytes(StandardCharsets.US_ASCII), issuerDigest,
                Timestamp.from(FIRST_SEEN));
    }
}
