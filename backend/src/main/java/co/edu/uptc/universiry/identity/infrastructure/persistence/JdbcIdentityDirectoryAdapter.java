package co.edu.uptc.universiry.identity.infrastructure.persistence;

import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcIdentityDirectoryAdapter implements IdentityDirectory {

    private static final int MAX_SEARCH_RESULTS = 100;

    private final JdbcTemplate jdbcTemplate;

    public JdbcIdentityDirectoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public RegisteredIdentity registerIfAbsent(AuthenticatedPrincipal principal, Instant firstSeenAt) {
        if (principal == null || firstSeenAt == null) {
            throw new IllegalArgumentException("authenticated identity and registration time are required");
        }
        Optional<RegisteredIdentity> current = findByDigest(principal, false);
        if (current.isPresent()) {
            return current.get();
        }

        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO university_user (user_id, created_at) VALUES (?, ?)",
                userId.toString(), Timestamp.from(firstSeenAt));
        try {
            jdbcTemplate.update("""
                    INSERT INTO institutional_identity (
                        identity_id, user_id, issuer, subject, issuer_sha256, first_seen_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), userId.toString(), principal.issuer(),
                    subjectBytes(principal.subject()),
                    sha256(principal.issuer()), Timestamp.from(firstSeenAt));
        } catch (DuplicateKeyException duplicate) {
            Optional<RegisteredIdentity> existing = findByDigest(principal, true);
            if (existing.isEmpty()) {
                throw duplicate;
            }
            if (!existing.get().principal().equals(principal)) {
                throw new IllegalStateException("identity issuer digest collision detected", duplicate);
            }
            jdbcTemplate.update("""
                    DELETE FROM university_user
                    WHERE user_id = ?
                      AND NOT EXISTS (
                          SELECT 1 FROM institutional_identity WHERE user_id = ?
                      )
                    """, userId.toString(), userId.toString());
            return existing.get();
        }
        return findByDigest(principal, false).orElseThrow(
                () -> new IllegalStateException("registered identity could not be read back"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegisteredIdentity> find(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("authenticated identity is required");
        }
        return findByDigest(principal, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegisteredIdentity> findBySubjectPrefix(String subjectPrefix, int limit) {
        byte[] prefix = validateSearchPrefix(subjectPrefix);
        if (limit < 1 || limit > MAX_SEARCH_RESULTS) {
            throw new IllegalArgumentException("identity search limit must be between 1 and 100");
        }
        byte[] exclusiveUpperBound = prefixUpperBound(prefix);
        if (exclusiveUpperBound == null) {
            return List.copyOf(jdbcTemplate.query("""
                    SELECT identity_id, user_id, issuer, subject, first_seen_at
                    FROM institutional_identity
                    WHERE subject >= ?
                    ORDER BY subject
                    LIMIT ?
                    """, this::mapIdentity, prefix, limit));
        }
        return List.copyOf(jdbcTemplate.query("""
                SELECT identity_id, user_id, issuer, subject, first_seen_at
                FROM institutional_identity
                WHERE subject >= ? AND subject < ?
                ORDER BY subject
                LIMIT ?
                """, this::mapIdentity, prefix, exclusiveUpperBound, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("canonical user id is required");
        }
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM university_user WHERE user_id = ?)",
                Boolean.class, userId.toString()));
    }

    private Optional<RegisteredIdentity> findByDigest(AuthenticatedPrincipal principal, boolean currentRead) {
        String lockingClause = currentRead ? " FOR UPDATE" : "";
        List<RegisteredIdentity> matches = jdbcTemplate.query("""
                SELECT identity_id, user_id, issuer, subject, first_seen_at
                FROM institutional_identity
                WHERE issuer_sha256 = ? AND subject = ?
                """ + lockingClause, this::mapIdentity, sha256(principal.issuer()), subjectBytes(principal.subject()));
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        RegisteredIdentity match = matches.getFirst();
        if (!match.principal().equals(principal)) {
            throw new IllegalStateException("identity issuer digest collision detected");
        }
        return Optional.of(match);
    }

    private RegisteredIdentity mapIdentity(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RegisteredIdentity(
                UUID.fromString(resultSet.getString("identity_id")),
                UUID.fromString(resultSet.getString("user_id")),
                new AuthenticatedPrincipal(resultSet.getString("issuer"),
                        new String(resultSet.getBytes("subject"), StandardCharsets.US_ASCII)),
                resultSet.getTimestamp("first_seen_at").toInstant());
    }

    private static byte[] validateSearchPrefix(String prefix) {
        if (prefix == null || prefix.isBlank() || prefix.length() > 255
                || prefix.chars().anyMatch(character -> character < 0x20 || character > 0x7e)) {
            throw new IllegalArgumentException("subject prefix must contain 1 to 255 printable ASCII characters");
        }
        return prefix.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] prefixUpperBound(byte[] prefix) {
        for (int index = prefix.length - 1; index >= 0; index--) {
            int character = Byte.toUnsignedInt(prefix[index]);
            if (character < 0x7e) {
                byte[] upperBound = new byte[index + 1];
                System.arraycopy(prefix, 0, upperBound, 0, index);
                upperBound[index] = (byte) (character + 1);
                return upperBound;
            }
        }
        return null;
    }

    private static byte[] subjectBytes(String subject) {
        return subject.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
