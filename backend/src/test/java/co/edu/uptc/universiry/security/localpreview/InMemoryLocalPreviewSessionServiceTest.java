package co.edu.uptc.universiry.security.localpreview;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLocalPreviewSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");

    @Test
    void issues_unique_opaque_sessions_that_decode_only_until_expiry_and_revoke() {
        // Arrange
        MutableClock clock = new MutableClock(NOW);
        var service = new InMemoryLocalPreviewSessionService(clock, new SecureRandom(), Duration.ofHours(4), 2);

        // Act
        LocalPreviewIssuedSession first = service.issue();
        LocalPreviewIssuedSession second = service.issue();
        var decoded = service.decode(first.accessToken());

        // Assert
        assertNotEquals(first.accessToken(), second.accessToken());
        assertEquals(43, first.accessToken().length());
        assertEquals(NOW.plus(Duration.ofHours(4)), first.expiresAt());
        assertEquals("https://local-preview.universiry.invalid", decoded.getIssuer().toString());
        assertEquals("local-preview-developer", decoded.getSubject());
        assertThrows(BadJwtException.class, () -> service.decode("not-a-session"));

        clock.advance(Duration.ofHours(4));
        assertThrows(BadJwtException.class, () -> service.decode(first.accessToken()));
        service.revoke(second.accessToken());
        assertThrows(BadJwtException.class, () -> service.decode(second.accessToken()));
    }

    @Test
    void caps_live_sessions_and_allows_new_ones_after_expired_sessions_are_removed() {
        // Arrange
        MutableClock clock = new MutableClock(NOW);
        var service = new InMemoryLocalPreviewSessionService(clock, new SecureRandom(), Duration.ofHours(4), 1);
        service.issue();

        // Act + Assert
        assertThrows(LocalPreviewSessionLimitException.class, service::issue);
        clock.advance(Duration.ofHours(4));
        assertTrue(service.issue().expiresAt().isAfter(clock.instant()));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant initial) {
            instant = new AtomicReference<>(initial);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(value -> value.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
