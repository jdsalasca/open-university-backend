package co.edu.uptc.universiry.identity.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "universiry.mysql-migration.enabled", matches = "true")
class CanonicalUserMigrationMySqlContractTest {

    private static final Instant FIRST_SEEN = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void mysql84_backfills_legacy_access_records_before_removing_identity_foreign_keys() throws Exception {
        // Arrange
        String adminUrl = requiredEnvironment("UNIVERSIRY_MYSQL_MIGRATION_ADMIN_URL");
        String username = requiredEnvironment("UNIVERSIRY_MYSQL_MIGRATION_USERNAME");
        String password = requiredEnvironment("UNIVERSIRY_MYSQL_MIGRATION_PASSWORD");
        String database = "universiry_user_v21_" + UUID.randomUUID().toString().replace("-", "");
        String databaseUrl = withDatabase(adminUrl, database);
        try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "`");
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseUrl, username, password);
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("20")).load().migrate();
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
                    ) VALUES (?, ?, 'TEACHER', 'ACTIVE', '2026-10-01', NULL, ?, ?, ?, 1)
                    """, assignmentId.toString(), targetIdentityId.toString(), "Synthetic minute 2026-42",
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
                    Timestamp.from(FIRST_SEEN), "Synthetic minute 2026-42");

            // Act
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

            // Assert
            assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8.4."));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM university_user", Integer.class));
            assertEquals(targetIdentityId.toString(), jdbc.queryForObject(
                    "SELECT target_user_id FROM identity_role_assignment WHERE assignment_id = ?", String.class,
                    assignmentId.toString()));
            assertEquals(grantorIdentityId.toString(), jdbc.queryForObject(
                    "SELECT actor_user_id FROM identity_access_audit_event WHERE audit_event_id = ?", String.class,
                    auditEventId.toString()));
            assertEquals(grantorIdentityId.toString(), jdbc.queryForObject(
                    "SELECT actor_identity_id FROM identity_access_audit_event WHERE audit_event_id = ?", String.class,
                    auditEventId.toString()));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'identity_role_assignment'
                      AND column_name = 'target_identity_id'
                    """, Integer.class));
        } finally {
            try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
                 var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE `" + database + "`");
            }
        }
    }

    private static void insertIdentity(JdbcTemplate jdbc, UUID identityId, String subject) throws Exception {
        String issuer = "https://identity.example.edu";
        byte[] issuerDigest = MessageDigest.getInstance("SHA-256").digest(issuer.getBytes(StandardCharsets.UTF_8));
        jdbc.update("""
                INSERT INTO institutional_identity (
                    identity_id, issuer, subject, issuer_sha256, first_seen_at
                ) VALUES (?, ?, ?, ?, ?)
                """, identityId.toString(), issuer, subject.getBytes(StandardCharsets.US_ASCII),
                issuerDigest, Timestamp.from(FIRST_SEEN));
    }

    private static String withDatabase(String serverUrl, String database) {
        int queryStart = serverUrl.indexOf('?');
        String base = queryStart < 0 ? serverUrl : serverUrl.substring(0, queryStart);
        String query = queryStart < 0 ? "" : serverUrl.substring(queryStart);
        if (!base.startsWith("jdbc:mysql://") || !base.endsWith("/")) {
            throw new IllegalArgumentException("migration contract admin URL must target the MySQL server root");
        }
        return base + database + query;
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required MySQL migration contract variable is missing: " + key);
        }
        return value;
    }
}
