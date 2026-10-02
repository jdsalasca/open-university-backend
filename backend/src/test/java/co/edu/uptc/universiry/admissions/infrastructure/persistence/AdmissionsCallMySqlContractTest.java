package co.edu.uptc.universiry.admissions.infrastructure.persistence;

import co.edu.uptc.universiry.admissions.application.AdmissionsCallRepository;
import co.edu.uptc.universiry.admissions.domain.AdmissionsActor;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallStatus;
import co.edu.uptc.universiry.admissions.domain.AdmissionsMilestone;
import co.edu.uptc.universiry.admissions.domain.AdmissionsMilestoneKind;
import co.edu.uptc.universiry.admissions.domain.AdmissionsSource;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "universiry.mysql-contract.enabled", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdmissionsCallMySqlContractTest {

    @Autowired
    private AdmissionsCallRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID callId;

    @DynamicPropertySource
    static void mysqlDatasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_URL"));
        properties.add("spring.datasource.username", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Test
    void mysql_84_persists_and_reads_only_the_current_published_revision() throws Exception {
        // Arrange
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        assertTrue(version != null && version.startsWith("8.4."), "MySQL 8.4 is required for this contract.");
        UUID userId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        String issuer = "https://issuer.example.edu/" + identityId;
        byte[] subject = ("contract-" + identityId).getBytes(StandardCharsets.UTF_8);
        Instant now = Instant.parse("2026-10-01T12:00:00Z");
        jdbcTemplate.update("INSERT INTO university_user (user_id, created_at) VALUES (?, ?)", userId.toString(), now);
        jdbcTemplate.update("""
                INSERT INTO institutional_identity
                    (identity_id, issuer, subject, issuer_sha256, first_seen_at, user_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, identityId.toString(), issuer, subject, sha256(issuer), now, userId.toString());
        AdmissionsActor actor = new AdmissionsActor(userId, identityId);
        callId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        String callKey = "mysql-contract-" + UUID.randomUUID().toString().substring(0, 8);
        AdmissionsSource source = new AdmissionsSource("Calendario sintético", "https://example.edu/calendar");
        AdmissionsMilestone milestone = new AdmissionsMilestone("registration", AdmissionsMilestoneKind.APPLICATION,
                LocalDate.parse("2027-01-01"), LocalDate.parse("2027-01-05"), "Inscripción", "Hito sintético.");
        AdmissionsCallContent content = new AdmissionsCallContent("Convocatoria sintética", "2027-I",
                LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-01"), source, source, List.of(milestone));

        // Act
        repository.createCall(callId, callKey, revisionId, content, actor, now);
        assertTrue(repository.findPublicCalls().isEmpty(), "Drafts must not appear in the public query.");
        repository.publish(callId, revisionId, 1, null, "Acto sintético 2027-I", actor, now.plusSeconds(1));

        // Assert
        var published = repository.findPublicCalls();
        assertEquals(1, published.size());
        assertEquals(callId, published.getFirst().callId());
        assertEquals(revisionId, published.getFirst().revisionId());
        assertEquals(1, published.getFirst().content().milestones().size());
        assertEquals("Acto sintético 2027-I", published.getFirst().officialReference());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admissions_call_audit_event WHERE call_id = ?", Integer.class, callId.toString()));
        assertEquals(AdmissionsCallStatus.PUBLISHED,
                repository.findAdminCall(callId).orElseThrow().publishedRevision().status());
    }

    @AfterTransaction
    void rolls_back_the_synthetic_call_after_the_contract_test() {
        if (callId != null) {
            assertEquals(0, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM admissions_call WHERE call_id = ?", Integer.class, callId.toString()));
        }
    }

    private static byte[] sha256(String value) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " when enabling the opt-in MySQL contract tests.");
        }
        return value;
    }
}
